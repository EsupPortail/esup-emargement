package org.esupportail.emargement.services;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.domain.MotifAbsence.StatutAbsence;
import org.esupportail.emargement.domain.MotifAbsence.TypeAbsence;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AbsenceService {
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Resource
	LogService logService;
	
	@Resource
	StoredFileService storedFileService;
	
	@Autowired
    private MessageSource messageSource;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Transactional
	public int updateMotifAbsence(String emargementcontext) {
		int nb= 0;
		List<MotifAbsence> list = motifAbsenceRepository.findByContextKey(emargementcontext);
		if(list.isEmpty()) {
			for(int i=1; i<5; i++) {
				MotifAbsence motifAbsence = new MotifAbsence();
				Context ctx = contextRepository.findByKey(emargementcontext);
				motifAbsence.setContext(ctx);
				motifAbsence.setLibelle(messageSource.getMessage("motifAbsence." + i + ".libelle", null, null));
				motifAbsence.setIsActif(true);
				motifAbsence.setStatutAbsence(StatutAbsence.valueOf(messageSource.getMessage("motifAbsence." + i + ".statut", null, null)));
				motifAbsence.setTypeAbsence(TypeAbsence.valueOf(messageSource.getMessage("motifAbsence." + i + ".type", null, null)));
				motifAbsenceRepository.save(motifAbsence);
				nb++;
			}
			log.info("Ajout de types de session : " + StringUtils.join(list, ", "));
			logService.log(ACTION.CREATE_MOTIF_ABSENCE, RETCODE.SUCCESS, "Ajout de motifs d'absence : " + StringUtils.join(list, ", "), null,  null, "all", null);
		}
		return nb;
	}
	
	@Transactional
	public Absence createAbsence(MotifAbsence motifAbsence, TagCheck tc, Absence absence) throws IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Context ctx = tc.getContext();
		absence.setContext(ctx);
		SessionEpreuve se = tc.getSessionEpreuve();
		absence.setDateDebut(se.getDateExamen());
		absence.setDateFin(se.getDateFin());
		absence.setHeureDebut(se.getHeureEpreuve());
		absence.setHeureFin(se.getFinEpreuve());
		absence.setPerson(tc.getPerson());
		absence.setCommentaire(absence.getCommentaire());
		absence.setMotifAbsence(motifAbsence);
        absence.setUserApp(userAppRepository.findByEppnAndContextKey(auth.getName(), tc.getContext().getKey()));
        if(absence.getFiles() != null && !absence.getFiles().isEmpty()) {
			for(MultipartFile file : absence.getFiles()) {
				if(file.getSize()>0) {
					StoredFile sf = new StoredFile();
					sf.setAbsence(absence);
					storedFileService.setStoredFile(sf, file, ctx.getKey(), null);
					storedFileRepository.save(sf);
				}
			}
		}
		absenceRepository.save(absence);
		return absence;
	}
}
