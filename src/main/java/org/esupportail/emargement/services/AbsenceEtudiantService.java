package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.Absence.OrigineAbsence;
import org.esupportail.emargement.domain.Absence.StatutValidation;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AbsenceEtudiantService {

	private static final List<String> CONTENT_TYPES_AUTORISES = Arrays.asList("application/pdf", "image/jpeg", "image/png");

	@Autowired
	AppliConfigService appliConfigService;

	@Autowired
	AbsenceService absenceService;

	@Autowired
	AbsenceRepository absenceRepository;

	@Autowired
	StoredFileRepository storedFileRepository;

	@Autowired
	StoredFileService storedFileService;

	@Autowired
	EmailService emailService;

	@Autowired
	LdapService ldapService;

	@Autowired
	MessageSource messageSource;

	@Value("${app.url}")
	private String appUrl;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public boolean isEnabled(Context ctx) {
		return ctx != null && Boolean.TRUE.equals(appliConfigService.isAbsenceEtudiantEnabled(ctx));
	}

	// Date la plus ancienne que peut avoir la fin d'une absence déclarée, au format yyyy-MM-dd.
	public String getDateMinFin(Context ctx) {
		return getLimiteFin(ctx).toString();
	}

	public List<Absence> getDeclarationsEtudiant(Context ctx, String eppn) {
		return absenceRepository.findByContextAndPersonEppnAndOrigineOrderByDateModificationDesc(ctx, eppn, OrigineAbsence.ETUDIANT);
	}

	// Déclaration non validée (en attente ou refusée) la plus pertinente pour chaque session affichée.
	public Map<Long, Absence> getDeclarationsEnCours(Context ctx, String eppn, Collection<TagCheck> tcs) {
		Map<Long, Absence> result = new HashMap<>();
		if(tcs == null || tcs.isEmpty()) {
			return result;
		}
		List<Absence> declarations = absenceRepository.findByContextAndPersonEppnAndOrigineAndStatutValidationNot(ctx, eppn,
				OrigineAbsence.ETUDIANT, StatutValidation.VALIDEE);
		for(TagCheck tc : tcs) {
			Absence retenue = null;
			for(Absence declaration : declarations) {
				if(absenceService.absenceCouvreSession(declaration, tc.getSessionEpreuve()) && isPlusPertinente(declaration, retenue)) {
					retenue = declaration;
				}
			}
			if(retenue != null) {
				result.put(tc.getId(), retenue);
			}
		}
		return result;
	}

	// Retourne le message d'erreur à afficher à l'étudiant, ou null si la déclaration est acceptable.
	public String verifierDeclaration(Person person, Context ctx, MotifAbsence motif, Date dateDebut, Date dateFin, List<MultipartFile> files) {
		if(!isEnabled(ctx)) {
			return "La déclaration d'absence n'est pas activée.";
		}
		if(person == null) {
			return "Vous n'êtes pas connu de cet environnement.";
		}
		if(dateDebut == null || dateFin == null) {
			return "Les dates de début et de fin sont obligatoires.";
		}
		if(toLocalDate(dateFin).isBefore(toLocalDate(dateDebut))) {
			return "La date de fin ne peut pas être antérieure à la date de début.";
		}
		LocalDate limite = getLimiteFin(ctx);
		if(toLocalDate(dateFin).isBefore(limite)) {
			return "Vous ne pouvez plus déclarer une absence terminée avant le " + limite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".";
		}
		if(!absenceRepository.findOverlappingAbsences(person, dateDebut, dateFin, getHeure(0, 0), getHeure(23, 59), ctx, StatutValidation.EN_ATTENTE).isEmpty()) {
			return "Une déclaration est déjà en attente de traitement pour tout ou partie de cette période.";
		}
		List<Absence> existantes = absenceRepository.findOverlappingAbsences(person, dateDebut, dateFin, getHeure(0, 0), getHeure(23, 59), ctx);
		if(!existantes.isEmpty()) {
			return "Une absence est déjà enregistrée sur tout ou partie de cette période (" + existantes.stream()
					.map(existante -> "du " + formatDate(existante.getDateDebut()) + " au " + formatDate(existante.getDateFin()))
					.collect(Collectors.joining(", ")) + "). Déclarez uniquement les jours non couverts.";
		}
		if(motif == null || !Boolean.TRUE.equals(motif.getIsActif()) || !Boolean.TRUE.equals(motif.getIsEtudiantVisible())
				|| motif.getContext() == null || !ctx.getId().equals(motif.getContext().getId())) {
			return "Motif d'absence invalide.";
		}
		int nbFichiers = 0;
		if(files != null) {
			for(MultipartFile file : files) {
				if(file.getSize() > 0) {
					if(!CONTENT_TYPES_AUTORISES.contains(file.getContentType())) {
						return "Format de justificatif non accepté (PDF, JPEG ou PNG uniquement).";
					}
					nbFichiers++;
				}
			}
		}
		if(nbFichiers == 0 && Boolean.TRUE.equals(appliConfigService.isAbsenceEtudiantJustificatifObligatoire(ctx))) {
			return "Un justificatif est obligatoire.";
		}
		return null;
	}

	// L'absence couvre des journées entières et n'est rattachée aux émargements qu'à sa validation par un gestionnaire.
	@Transactional
	public Absence declarer(Person person, Context ctx, MotifAbsence motif, String commentaire, Date dateDebut, Date dateFin,
			List<MultipartFile> files) throws IOException {
		Absence absence = new Absence();
		absence.setContext(ctx);
		absence.setPerson(person);
		absence.setMotifAbsence(motif);
		absence.setCommentaire(StringUtils.trimToNull(commentaire));
		absence.setDateDebut(dateDebut);
		absence.setDateFin(dateFin);
		absence.setHeureDebut(getHeure(0, 0));
		absence.setHeureFin(getHeure(23, 59));
		absence.setDateModification(new Date());
		absence.setOrigine(OrigineAbsence.ETUDIANT);
		absence.setStatutValidation(StatutValidation.EN_ATTENTE);
		absence = absenceRepository.save(absence);
		if(files != null) {
			for(MultipartFile file : files) {
				if(file.getSize() > 0) {
					StoredFile sf = new StoredFile();
					sf.setAbsence(absence);
					storedFileService.setStoredFile(sf, file, ctx.getKey(), null);
					storedFileRepository.save(sf);
				}
			}
		}
		return absence;
	}

	public Long countEnAttente(Context ctx) {
		return absenceRepository.countByContextAndOrigineAndStatutValidation(ctx, OrigineAbsence.ETUDIANT, StatutValidation.EN_ATTENTE);
	}

	public List<Absence> getDeclarations(Context ctx, StatutValidation statut) {
		return absenceRepository.findByContextAndOrigineAndStatutValidationOrderByDateModificationDesc(ctx, OrigineAbsence.ETUDIANT, statut);
	}

	// Absences validées que la validation de cette déclaration remplacerait.
	public List<Absence> getAbsencesRemplacees(Absence declaration) {
		return absenceRepository.findOverlappingAbsences(declaration.getPerson(), declaration.getDateDebut(), declaration.getDateFin(),
				declaration.getHeureDebut(), declaration.getHeureFin(), declaration.getContext());
	}

	public boolean isTraitable(Absence absence, Context ctx) {
		return absence != null && ctx != null && absence.getContext() != null && ctx.getId().equals(absence.getContext().getId())
				&& OrigineAbsence.ETUDIANT.equals(absence.getOrigine()) && StatutValidation.EN_ATTENTE.equals(absence.getStatutValidation());
	}

	@Transactional
	public void valider(Absence absence, UserApp traitePar) {
		marquerTraitee(absence, traitePar, StatutValidation.VALIDEE);
		absenceRepository.save(absence);
		absenceService.rattacherAbsence(absence);
		notifierTraitement(absence);
	}

	@Transactional
	public void refuser(Absence absence, UserApp traitePar, String motifRefus) {
		marquerTraitee(absence, traitePar, StatutValidation.REFUSEE);
		absence.setMotifRefus(StringUtils.trimToNull(motifRefus));
		absenceRepository.save(absence);
		notifierTraitement(absence);
	}

	public void notifierDeclaration(Absence absence, Context ctx) {
		if(!Boolean.TRUE.equals(appliConfigService.isAbsenceEtudiantNotifGestionnaire(ctx))) {
			return;
		}
		List<String> destinataires = appliConfigService.getListeGestionnaires(ctx);
		if(destinataires.isEmpty()) {
			return;
		}
		String nom = ldapService.getPrenomNom(absence.getPerson().getEppn());
		Object[] args = {nom, formatDate(absence.getDateDebut()), formatDate(absence.getDateFin()), absence.getMotifAbsence().getLibelle(),
				String.valueOf(storedFileRepository.countByAbsence(absence)), appUrl + "/" + ctx.getKey() + "/manager/absence/declarations"};
		envoyerApresCommit(destinataires, getMessage("absenceetudiant.mail.declaration.sujet", args), getMessage("absenceetudiant.mail.declaration.corps", args));
	}

	private void notifierTraitement(Absence absence) {
		List<LdapUser> ldapUsers = ldapService.getUsers(absence.getPerson().getEppn());
		if(ldapUsers.isEmpty() || StringUtils.isBlank(ldapUsers.get(0).getEmail())) {
			log.info("Pas de notification de traitement : adresse email introuvable pour " + absence.getPerson().getEppn());
			return;
		}
		LdapUser user = ldapUsers.get(0);
		boolean validee = StatutValidation.VALIDEE.equals(absence.getStatutValidation());
		String prefixe = validee ? "absenceetudiant.mail.validee" : "absenceetudiant.mail.refusee";
		String motifRefus = StringUtils.isBlank(absence.getMotifRefus()) ? "" : getMessage("absenceetudiant.mail.refusee.motif", absence.getMotifRefus());
		Object[] args = {user.getPrenomNom(), formatDate(absence.getDateDebut()), formatDate(absence.getDateFin()), absence.getMotifAbsence().getLibelle(), motifRefus};
		envoyerApresCommit(Collections.singletonList(user.getEmail()), getMessage(prefixe + ".sujet", args), getMessage(prefixe + ".corps", args));
	}

	private void envoyerApresCommit(List<String> destinataires, String sujet, String corps) {
		Runnable envoi = () -> {
			for(String destinataire : destinataires) {
				try {
					log.info("Notification d'absence à " + destinataire + " : " + sujet);
					emailService.sendSimpleMessage(destinataire, sujet, corps, new String[0]);
				} catch (Exception e) {
					log.warn("Echec de l'envoi de la notification d'absence à " + destinataire, e);
				}
			}
		};
		if(TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					envoi.run();
				}
			});
		} else {
			envoi.run();
		}
	}

	private String getMessage(String code, Object... args) {
		return messageSource.getMessage(code, args, null);
	}

	private String formatDate(Date date) {
		return new SimpleDateFormat("dd/MM/yyyy").format(date);
	}

	private void marquerTraitee(Absence absence, UserApp traitePar, StatutValidation statut) {
		Date now = new Date();
		absence.setStatutValidation(statut);
		absence.setTraitePar(traitePar);
		absence.setDateTraitement(now);
		absence.setDateModification(now);
	}

	private boolean isPlusPertinente(Absence candidate, Absence current) {
		if(current == null) {
			return true;
		}
		boolean candidateEnAttente = StatutValidation.EN_ATTENTE.equals(candidate.getStatutValidation());
		boolean currentEnAttente = StatutValidation.EN_ATTENTE.equals(current.getStatutValidation());
		if(candidateEnAttente != currentEnAttente) {
			return candidateEnAttente;
		}
		return candidate.getDateModification() != null
				&& (current.getDateModification() == null || candidate.getDateModification().after(current.getDateModification()));
	}

	private LocalDate getLimiteFin(Context ctx) {
		return LocalDate.now().minusDays(appliConfigService.getAbsenceEtudiantDelaiJours(ctx));
	}

	// Heure stockée comme celles de la saisie manuelle (jour de référence de l'époque, fuseau local).
	private Date getHeure(int heure, int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(Calendar.HOUR_OF_DAY, heure);
		calendar.set(Calendar.MINUTE, minute);
		return calendar.getTime();
	}

	private LocalDate toLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
