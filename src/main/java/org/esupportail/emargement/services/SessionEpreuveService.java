package org.esupportail.emargement.services;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.PropertiesForm;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.StatutSession;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StatutSessionRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.services.AppliConfigService.AppliConfigKey;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class SessionEpreuveService {
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	StatutSessionRepository statutSessionRepository;

	@Autowired
	private CampusRepository campusRepository;
	
	@Autowired
	private SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	TypeSessionRepository typeSessionRepository;
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	private AppliConfigRepository appliConfigRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Resource
	SessionLocationService sessionLocationService;
	
	@Resource
	EsupSignatureService esupSignatureService;
		
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	StoredFileService storedFileService;
	
	@Resource
	ContextService contextService;
	
	@Resource
	LogService logService;
	
	@Resource
	ImportExportService importExportService;
	
	@Value("${app.url}")
	private String appUrl;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
    private MessageSource messageSource;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void computeCounters(List<SessionEpreuve> sessionEpreuveList) {
		for(SessionEpreuve session : sessionEpreuveList) {
			session.setNbLieuxSession(sessionLocationRepository.countBySessionEpreuveId(session.getId()));
			Long nbTagCheckerSession = Long.valueOf("0");
			List<TagChecker> tcs =tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(session.getId());
			if(!tcs.isEmpty()) {
				List<UserApp>  userApps = tcs.stream() .map(l -> l.getUserApp()).distinct().collect(Collectors.toList());
				nbTagCheckerSession = new Long(userApps.size());
			}
			Long countDispatch = tagCheckService.countNbTagCheckRepartitionNotNull(session.getId(), true) + tagCheckService.countNbTagCheckRepartitionNotNull(session.getId(), false);
			session.setNbDispatchTagCheck(countDispatch);
			session.setNbPresentsSession(tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNullAndSessionLocationExpectedIsNotNull(session.getId()));
			session.setNbTagCheckerSession(nbTagCheckerSession);
			Long unknown = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNotNull(session.getId());
			session.setNbInscritsSession(tagCheckRepository.countBySessionEpreuveId(session.getId())-unknown);
			session.setDureeEpreuve(toolUtil.getDureeEpreuve(session.getHeureEpreuve(), session.getFinEpreuve(), null));
			session.setNbCheckedByCardTagCheck(tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsCheckedByCardTrue(session.getId(), TypeEmargement.CARD.name(), session.getContext().getId()));
			session.setNbStoredFiles(storedFileRepository.countBySessionEpreuve(session));
			session.setNbUnknown(unknown);
		}
	}

   public boolean executeRepartition(Long sessionEpreuveId, String tagCheckOrder) {
        if (tagCheckOrder == null) tagCheckOrder = "alpha";

        // Comptages
        Long countTiers = tagCheckService.countNbTagCheckRepartitionNull(sessionEpreuveId, true);
        Long countNonTiers = tagCheckService.countNbTagCheckRepartitionNull(sessionEpreuveId, false);

        Long repartisTiers = tagCheckService.countNbTagCheckRepartitionNotNull(sessionEpreuveId, true);
        Long repartisNonTiers = tagCheckService.countNbTagCheckRepartitionNotNull(sessionEpreuveId, false);

        int capaciteTiers = countCapaciteTotalSessionLocations(sessionEpreuveId, true);
        int capaciteNonTiers = countCapaciteTotalSessionLocations(sessionEpreuveId, false);

        Long restantTiers = capaciteTiers - repartisTiers;
        Long restantNonTiers = capaciteNonTiers - repartisNonTiers;

        // Conditions de faisabilité
        boolean possibleSepare = countTiers <= restantTiers && countNonTiers <= restantNonTiers;
        boolean possibleSansSalleTT = (countTiers + countNonTiers <= restantNonTiers && capaciteTiers == 0);

        if (!possibleSepare && !possibleSansSalleTT) {
            return true; // isOver = true
        }

        SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(sessionEpreuveId).get();
        int j = 0;

        if (possibleSansSalleTT) {
            // Tous ensemble
            List<SessionLocation> allLocations =
                    sessionLocationRepository.findBySessionEpreuveIdOrderByPriorite(sessionEpreuveId);
            j = repartirTagChecks(allLocations, sessionEpreuveId, null, tagCheckOrder, sessionEpreuve, j);
        } else {
            // Tiers-temps
            List<SessionLocation> locationsTT =
                    sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyTrueOrderByPriorite(sessionEpreuveId);
            j = repartirTagChecks(locationsTT, sessionEpreuveId, true, tagCheckOrder, sessionEpreuve, j);

            // Non tiers-temps
            List<SessionLocation> locationsNonTT =
                    sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyFalseOrderByPriorite(sessionEpreuveId);
            j = repartirTagChecks(locationsNonTT, sessionEpreuveId, false, tagCheckOrder, sessionEpreuve, j);
        }

        return false; // isOver = false
    }

    /**
     * Répartit les étudiants dans une liste de salles en respectant l’ordre demandé.
     */
    private int repartirTagChecks(
            List<SessionLocation> sessionLocations,
            Long sessionEpreuveId,
            Boolean isTiersTemps,
            String tagCheckOrder,
            SessionEpreuve sessionEpreuve,
            int startIndex) {

        int j = startIndex;

        for (SessionLocation sl : sessionLocations) {
            // Nombre de places déjà occupées
            Long nbUsedPlace = countByLocation(sessionEpreuveId, sl.getId(), isTiersTemps);

            // Étudiants en attente
            List<TagCheck> tagCheckList =
                    findWaitingTagChecks(sessionEpreuveId, isTiersTemps, tagCheckOrder);

            if (tagCheckList.isEmpty()) break;

            for (TagCheck tc : tagCheckList) {
                if (nbUsedPlace >= sl.getCapacite()) break;

                if (tc.getIsUnknown()) {
                    tagCheckRepository.delete(tc);
                } else {
                    tc.setSessionLocationExpected(sl);
                    tc.setNumAnonymat(constructNumAnonymat(sessionEpreuve, j));
                    tagCheckRepository.save(tc);
                    nbUsedPlace++;
                    j++;
                }
            }
        }
        return j;
    }

    /**
     * Récupère les TagChecks en attente selon les règles (tiers-temps, ordre).
     */
    private List<TagCheck> findWaitingTagChecks(Long sessionEpreuveId, Boolean isTiersTemps, String order) {
        List<TagCheck> results;

        if ("alpha".equals(order)) {
            if (isTiersTemps == null) {
                results = tagCheckRepository.findBySessionEpreuveIdAndSessionLocationExpectedIsNullOrderByPersonEppn(sessionEpreuveId);
            } else if (isTiersTemps) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrueOrderByPersonEppn(sessionEpreuveId);
            } else {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalseOrderByPersonEppn(sessionEpreuveId);
            }
            // Trie alpha : utiliser NomPrenom
            tagCheckService.setNomPrenomTagChecks(results, false, false);
            results = results.stream()
                    .sorted(Comparator.comparing(TagCheck::getNomPrenom))
                    .collect(Collectors.toList());
        } else if ("numEtu".equals(order)) {
            if (isTiersTemps == null) {
                results = tagCheckRepository.findBySessionEpreuveIdAndSessionLocationExpectedIsNullOrderByPersonNumIdentifiant(sessionEpreuveId);
            } else if (isTiersTemps) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrueOrderByPersonNumIdentifiant(sessionEpreuveId);
            } else {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalseOrderByPersonNumIdentifiant(sessionEpreuveId);
            }
        } else {
            if (isTiersTemps == null) {
                results = tagCheckRepository.findBySessionEpreuveIdAndSessionLocationExpectedIsNull(sessionEpreuveId);
            } else if (isTiersTemps) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(sessionEpreuveId);
            } else {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(sessionEpreuveId);
            }
        }

        return results;
    }

    /**
     * Compte le nombre de places déjà utilisées dans une salle.
     */
    private Long countByLocation(Long sessionEpreuveId, Long locationId, Boolean isTiersTemps) {
        if (isTiersTemps == null) {
            return tagCheckRepository.countBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndSessionLocationExpectedId(sessionEpreuveId, locationId);
        } else if (isTiersTemps) {
            return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrueAndSessionLocationExpectedId(sessionEpreuveId, locationId);
        } else {
            return tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalseAndSessionLocationExpectedId(sessionEpreuveId, locationId);
        }
    }

    public void affinageRepartition(PropertiesForm propertiesForm, String emargementContext, String tagCheckOrder) {
        if (tagCheckOrder == null) tagCheckOrder = "alpha";

        List<SessionLocation> list = propertiesForm.list;
        if (list.isEmpty()) return;

        SessionEpreuve se = list.get(0).getSessionEpreuve();

        // Reset des affectations existantes
        tagCheckService.resetSessionLocationExpected(se.getId());

        // Séparation des salles tiers-temps et non
        List<SessionLocation> isTiersTempsOnlyList = list.stream()
                .filter(SessionLocation::getIsTiersTempsOnly)
                .collect(Collectors.toList());

        List<SessionLocation> notTiersTempsList = list.stream()
                .filter(sl -> !sl.getIsTiersTempsOnly())
                .collect(Collectors.toList());

        if (isTiersTempsOnlyList.isEmpty()) {
            // Pas de salles tiers-temps → tout le monde dans notTiersTempsList
            List<TagCheck> tagCheckList = findAllTagChecks(se.getId(), null, tagCheckOrder);
            assignTagChecksToLocations(tagCheckList, notTiersTempsList);
        } else {
            // Tiers-temps
            List<TagCheck> tagCheckTiers = findAllTagChecks(se.getId(), true, tagCheckOrder);
            assignTagChecksToLocations(tagCheckTiers, isTiersTempsOnlyList);

            // Non tiers-temps
            List<TagCheck> tagCheckNonTiers = findAllTagChecks(se.getId(), false, tagCheckOrder);
            assignTagChecksToLocations(tagCheckNonTiers, notTiersTempsList);
        }

        logService.log(ACTION.AFFINER_REPARTITION, RETCODE.SUCCESS,
                "Session : " + se.getNomSessionEpreuve(), null, null, emargementContext, null);
        log.info("affinage session : " + se.getNomSessionEpreuve());
    }

    /**
     * Récupère tous les TagChecks (pas seulement "waiting"),
     * avec tri par ordre et filtre tiers-temps si besoin.
     */
    private List<TagCheck> findAllTagChecks(Long sessionEpreuveId, Boolean isTiersTemps, String order) {
        List<TagCheck> results;

        if ("alpha".equals(order)) {
            if (isTiersTemps == null) {
                results = tagCheckRepository.findBySessionEpreuveIdOrderByPersonEppn(sessionEpreuveId);
            } else if (isTiersTemps) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonEppn(sessionEpreuveId);
            } else {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonEppn(sessionEpreuveId);
            }
            // Trie alpha sur NomPrenom
            tagCheckService.setNomPrenomTagChecks(results, false, false);
            results = results.stream()
                    .sorted(Comparator.comparing(TagCheck::getNomPrenom))
                    .collect(Collectors.toList());
        } else if ("numEtu".equals(order)) {
            if (isTiersTemps == null) {
                results = tagCheckRepository.findBySessionEpreuveIdOrderByPersonNumIdentifiant(sessionEpreuveId);
            } else if (isTiersTemps) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonNumIdentifiant(sessionEpreuveId);
            } else {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonNumIdentifiant(sessionEpreuveId);
            }
        } else {
            if (isTiersTemps == null) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
            } else if (isTiersTemps) {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(sessionEpreuveId);
            } else {
                results = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(sessionEpreuveId);
            }
        }

        return results;
    }

    /**
     * Répartit une liste de TagChecks dans les salles fournies,
     * en respectant nbInscritsSessionLocation et capacité.
     */
    private void assignTagChecksToLocations(List<TagCheck> tagChecks, List<SessionLocation> locations) {
        if (tagChecks.isEmpty() || locations.isEmpty()) return;

        // Trier les salles par priorité croissante
        List<SessionLocation> sortedLocations = locations.stream()
                .sorted(Comparator.comparing(SessionLocation::getPriorite))
                .collect(Collectors.toList());

        int i = 0; // compteur dans la salle
        int j = 0; // index de salle courante
        SessionLocation sl = sortedLocations.get(j);

        for (TagCheck tc : tagChecks) {
            if (i < sl.getNbInscritsSessionLocation() && j < sortedLocations.size() && i < sl.getCapacite()) {
                tc.setSessionLocationExpected(sl);
                tagCheckRepository.save(tc);
                i++;
            } else {
                i = 1;
                j++;
                if (j >= sortedLocations.size()) break; // plus de salles dispo
                sl = sortedLocations.get(j);
                tc.setSessionLocationExpected(sl);
                tagCheckRepository.save(tc);
            }
        }
    }
    
    public String constructNumAnonymat(SessionEpreuve sessionEpreuve, int numSequentiel) {
    	//on construit le début du numéro d'anonymat  : ID_SESSION-ANNEE-MOIS-JOUR-NO_SEQUENTIEL
    	String numAnonymat = "";
    	String padsessionId = StringUtils.leftPad(String.valueOf(sessionEpreuve.getId()), 3, "0");
    	String sessionDate =  String.format("%1$tY-%1$tm-%1$td", sessionEpreuve.getDateExamen());
    	String padNumSeq = StringUtils.leftPad(String.valueOf(numSequentiel), 3, "0");
    	
    	numAnonymat = padsessionId.concat("-").concat(sessionDate).concat("-").concat(padNumSeq);
    	return numAnonymat;
    }
    
    public int countCapaciteTotalSessionLocations(Long sessionEpreuveId, boolean isTiersTemps) {
    	int capaciteTotal = 0;
    	List<SessionLocation> sessionLocationList= sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyFalseOrderByPriorite(sessionEpreuveId);
    	if(isTiersTemps) {
    		sessionLocationList= sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyTrueOrderByPriorite(sessionEpreuveId);
    	}
    	if(!sessionLocationList.isEmpty()) {
			for(SessionLocation sl:  sessionLocationList) {
				capaciteTotal += sl.getCapacite();
			}
		}
    	return capaciteTotal;
    }
	
    @Transactional
	public void save(SessionEpreuve sessionEpreuve, String emargementContext, List<MultipartFile> files) throws IOException {
    	if(files != null) {
    		sessionEpreuve.setFiles(files);
    	}
		sessionEpreuveRepository.save(sessionEpreuve);
		if(sessionEpreuve.getFiles() != null && !sessionEpreuve.getFiles().isEmpty()) {
			for(MultipartFile file : sessionEpreuve.getFiles()) {
				if(file.getSize()>0) {
					StoredFile sf = new StoredFile();
					storedFileService.setStoredFile(sf, file, emargementContext, sessionEpreuve);
					storedFileRepository.save(sf);
				}
			}
		}
	}
	
	public void exportEmargement(HttpServletResponse response,  Long sessionLocationId, Long sessionEpreuveId, String type, String emargementContext) {
		
		List<TagCheck> list = tagCheckRepository.findTagCheckBySessionLocationExpectedId(sessionLocationId);
		SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
		SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
		String dateFin = (se.getDateFin()!=null)? "_" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin())) : "";
    	String nomFichier = "Liste_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").
    			concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()).concat(dateFin));
    	nomFichier = nomFichier.replace(" ", "_").concat(".pdf");
		tagCheckService.setNomPrenomTagChecks(list, false, false);
		Collections.sort(list,  new Comparator<TagCheck>() {	
			@Override
            public int compare(TagCheck obj1, TagCheck obj2) {
				String nom1 = ""; String nom2 = "";
				if(obj1.getPerson() != null && obj1.getPerson().getNom() !=null) {
					nom1 = obj1.getPerson().getNom();
				}
				if(obj2.getPerson() != null && obj2.getPerson().getNom() !=null) {
						nom2 = obj2.getPerson().getNom();
				}
				if(obj1.getGuest() != null && obj1.getGuest().getNom() !=null) {
					nom1 = obj1.getGuest().getNom();
				}
				if(obj2.getGuest() != null && obj2.getGuest().getNom() !=null) {
						nom2 = obj2.getGuest().getNom();
				}
				return nom1.compareTo(nom2);
			}
		});
		PdfPTable table = null;
		if("Liste".equals(type)) {
			int nbColumn = (se.getIsProcurationEnabled())? 6 : 5;
	    	Long countTempsAmenage = tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(se.getId());
	    	if(countTempsAmenage>0) {
	    		nbColumn = nbColumn + 1;
			}
	    	if(BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
				nbColumn = nbColumn + 1;
			}
	    	table = new PdfPTable(nbColumn);
	    	
	    	table.setWidthPercentage(100);
	    	table.setHorizontalAlignment(Element.ALIGN_CENTER);
	    	
	        //On créer l'objet cellule.
	        String libelleSe = list.get(0).getSessionLocationExpected().getSessionEpreuve().getNomSessionEpreuve().concat(" ").
	        		concat(String.format("%1$td-%1$tm-%1$tY", (list.get(0).getSessionLocationExpected().getSessionEpreuve().getDateExamen())).concat(dateFin).
	        		concat(" à ").concat(list.get(0).getSessionLocationExpected().getLocation().getCampus().getSite()).concat(" --  ").
	        		concat(list.get(0).getSessionLocationExpected().getLocation().getNom()));
	        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
	        cell.setBackgroundColor(BaseColor.GREEN);
	        cell.setColspan(nbColumn);
	        table.addCell(cell);
	   
	        //contenu du tableau.
	        PdfPCell header1 = new PdfPCell(new Phrase("Identifiant")); header1.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header2 = new PdfPCell(new Phrase("Nom")); header2.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header3 = new PdfPCell(new Phrase("Prénom")); header3.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header31 = new PdfPCell(new Phrase("Groupe")); header31.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header4 = new PdfPCell(new Phrase("Type")); header4.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header5 = new PdfPCell(new Phrase("Tiers-temps")); header5.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header6 = new PdfPCell(new Phrase("Procuration")); header6.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header7 = new PdfPCell(new Phrase("Signature")); header7.setBackgroundColor(BaseColor.GRAY);
	        
	        table.addCell(header1);
	        table.addCell(header2);
	        table.addCell(header3);
	        if(BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
	        	table.addCell(header31);
	        }  
	        table.addCell(header4);
	        if(countTempsAmenage>0) {
	        	table.addCell(header5);
	        }
	        if(se.getIsProcurationEnabled()) {
	        	table.addCell(header6);
	        }
	        table.addCell(header7);
	        
	        if(!list.isEmpty()) {
	        	for(TagCheck tc : list) {
	        		String nom = "";
	        		String prenom = "";
	        		String identifiant = "";
	        		String typeIndividu = "";
	        		String signature = tc.getAbsence()!=null? tc.getAbsence().getMotifAbsence().getLibelle() : "";
	        		String groupe = "";
	        		if(tc.getPerson() !=null ) {
	        			nom = tc.getPerson().getNom();
	        			prenom = tc.getPerson().getPrenom();
	        			identifiant = tc.getPerson().getNumIdentifiant();
	        			if(identifiant == null) {
	        				identifiant = tc.getPerson().getEppn();
	        			}
	        			typeIndividu = messageSource.getMessage("person.type.".concat(tc.getPerson().getType()), null, null);
	        			if(tc.getPerson().getGroupes() != null) {
		        			List<String> groupes = tc.getPerson().getGroupes().stream().map(x -> x.getNom()).collect(Collectors.toList());
		        			groupe = StringUtils.join(groupes,",");
	        			}
	        		}else if(tc.getGuest() !=null ) {
	        			nom = tc.getGuest().getNom();
	        			prenom = tc.getGuest().getPrenom();
	        			identifiant = tc.getGuest().getEmail();
	        			typeIndividu = "Externe";
	        			if(tc.getGuest().getGroupes() != null) {
		        			List<String> groupes = tc.getGuest().getGroupes().stream().map(x -> x.getNom()).collect(Collectors.toList());
		        			groupe = StringUtils.join(groupes,",");
	        			}
	        		}
	        		PdfPCell cell1 = new PdfPCell(new Phrase(identifiant)); cell1.setMinimumHeight(30); 
	        		PdfPCell cell2 = new PdfPCell(new Phrase(nom)); cell2.setMinimumHeight(30);
	        		PdfPCell cell3 = new PdfPCell(new Phrase(prenom)); cell3.setMinimumHeight(30);
	        		PdfPCell cell31 = new PdfPCell(new Phrase(groupe)); cell31.setMinimumHeight(30);
	        		PdfPCell cell4 = new PdfPCell(new Phrase(typeIndividu)); cell4.setMinimumHeight(30);
	        		PdfPCell cell5 = new PdfPCell(new Phrase((tc.getIsTiersTemps())? "Oui": "Non")); cell5.setMinimumHeight(30);
	        		PdfPCell cell6 = new PdfPCell(new Phrase((tc.getProxyPerson()!=null)? tc.getProxyPerson().getNom(): "")); cell6.setMinimumHeight(30);
	        		PdfPCell cell7 = new PdfPCell(new Phrase(signature)); cell7.setMinimumHeight(30);
	        		
	    	        table.addCell(cell1);
	    	        table.addCell(cell2);
	    	        table.addCell(cell3);
	    	        if(BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
	    	        	table.addCell(cell31);
	    	        }
	    	        table.addCell(cell4);
	    	        if(countTempsAmenage>0) {
	    	        	table.addCell(cell5);
	    	        }
	    	        if(se.getIsProcurationEnabled()) {
	    	        	 table.addCell(cell6);
	    	        }
	    	        table.addCell(cell7);
	        	}
	        }
	       
		}
		
		 Document document = new Document();
        document.setMargins(40, 40, 40, 40);
        try 
        {
          response.setContentType("application/pdf");
          response.setHeader("Content-Disposition","attachment; filename=".concat(nomFichier));
          PdfWriter.getInstance(document,  response.getOutputStream());
          document.open();

          document.add(table);
          logService.log(ACTION.EXPORT_PDF, RETCODE.SUCCESS, "Export pdf Liste :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        } catch (DocumentException de) {
          de.printStackTrace();
        } catch (IOException de) {
          de.printStackTrace();
        }
       
        document.close();
    }
	
	@Scheduled(cron= "${emargement.sessions.statut.update}")
	@Transactional
	public void endedOrcloseSessions(){
		List<Context> contextList = contextRepository.findAll();
		if(!contextList.isEmpty()) {
			log.info("Début de mise à jour statut sessions");
			for(Context ctx : contextList) {
				String ctxKey = ctx.getKey();
				log.info("--Contexte : " + ctxKey);
				Date today = DateUtils.truncate(new Date(),  Calendar.DATE);
				List<AppliConfig> configs = appliConfigRepository.findAppliConfigByKeyAndContext(AppliConfigKey.AUTO_CLOSE_SESSION.name(), ctx);
				String [] keys = "true".equals(configs.get(0).getValue()) ? new String[]  {"PROCESSED", "CANCELLED", "CLOSED"} : 
					new String[] {"CLOSED", "PROCESSED", "CANCELLED", "ENDED"};
				List<SessionEpreuve> ses =  sessionEpreuveRepository.findByContextAndDateExamenLessThanAndStatutSessionKeyNotIn(ctx, today, Arrays.asList(keys));
				if(!ses.isEmpty()) {
					String value = configs.get(0).getValue();
					int i = 0;
					for (SessionEpreuve se : ses) {
						if("true".equals(value)) {
							se.setStatutSession(statutSessionRepository.findByKeyAndContext("CLOSED", ctx));
							i++;
						}else {
							se.setStatutSession(statutSessionRepository.findByKeyAndContext("ENDED", ctx));
							i++;
						}
						sessionEpreuveRepository.save(se);
					}
					if("true".equals(value)){
						logService.log(ACTION.CLOSE_SESSION, RETCODE.SUCCESS, "Sessions clôturées : " + i , null, null, ctxKey, null);
					}else {
						logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Statut Terminé - nb sessions : " + i, null, null, ctxKey, null);
					}
				}
				List<SessionEpreuve> sesToday = sessionEpreuveRepository.findByContextAndDateExamenAndStatutSessionKey(ctx, today,"STANDBY");
				if(!sesToday.isEmpty()) {
					int j = 0;
					for (SessionEpreuve seToday : sesToday) {
						seToday.setStatutSession(statutSessionRepository.findByKeyAndContext("OPENED", ctx));
						sessionEpreuveRepository.save(seToday);
						j++;
					}
					logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Statut Ouverte - nb sessions : " + j, null, null, ctxKey, null);
				}
			}
			log.info("Fin de mise à jour statut sessions");
		}
	}
	
	public List<SessionEpreuve> getListSessionEpreuveByTagchecker(String eppn, String key) {
		Page<TagChecker> tagCheckerList = tagCheckerRepository
				.findByUserAppEppnAndSessionLocationSessionEpreuveStatutSessionKeyAndSessionLocationSessionEpreuveAnneeUniv(eppn, key,
						String.valueOf(getCurrentanneUniv()), null);
		List<SessionEpreuve> newList = new ArrayList<>();
		for (TagChecker tc : tagCheckerList.getContent()) {
			SessionEpreuve se = tc.getSessionLocation().getSessionEpreuve();
			if(!newList.contains(se)){
				newList.add(se);
			}
		}
		return newList;
	}
	
	public List<String> getYears(String ctx) {
	    List<String> years = new ArrayList<>();

	    Context context = contextRepository.findByContextKey(ctx);
	    List<String> anneeUnivs;
	    if ("all".equals(ctx)) {
	        anneeUnivs = sessionEpreuveRepository.findDistinctAnneeUnivAll();
	    } else {
	        anneeUnivs = sessionEpreuveRepository.findDistinctAnneeUniv(context.getId());
	    }

	    if (anneeUnivs != null && !anneeUnivs.isEmpty()) {
	    	Set<Integer> uniqueYears = anneeUnivs.stream()
	    	        .map(Integer::valueOf)
	    	        .collect(Collectors.toCollection(() -> new TreeSet<Integer>(Comparator.reverseOrder())));
	  
	    	for (Integer year : uniqueYears) {
	            years.add(year + "/" + (year + 1));
	        }
	    }

	    return years;
	}

	public String getLastAnneeUniv(String ctx) {
		Context context = contextRepository.findByContextKey(ctx);
		List<String> anneeUnivs = sessionEpreuveRepository.findDistinctAnneeUnivOrderByDesc(context.getId());
		if (!anneeUnivs.isEmpty()) {
			return anneeUnivs.get(0);
		}
		return String.valueOf(getCurrentanneUniv());
	}
	
	public int getCurrentanneUniv() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		int currentYear = year;
		if(month<8) {
			currentYear = year-1;
		}
		return currentYear;
	}
	
	public int getCurrentAnneeUnivFromDate(Date date) {
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(date);

	    int year = cal.get(Calendar.YEAR);
	    int month = cal.get(Calendar.MONTH);

	    int currentYear = year;
	    if (month < Calendar.SEPTEMBER) {
	        currentYear = year - 1;
	    }

	    return currentYear;
	}
	
	public void duplicateAll(List<Long> idSessions, int jours){
		for(Long id : idSessions) {
			duplicateSessionEpreuve(id, true, jours);
		}
	}
	
	 public SessionEpreuve duplicateSessionEpreuve(Long id, boolean isSameName, int jours){
		SessionEpreuve originalSe = sessionEpreuveRepository.findById(id).get();
		Context context = originalSe.getContext();
        SessionEpreuve newSe = new SessionEpreuve();
        if(jours>0) {
        	if(jours == 100){
        		newSe.setDateExamen(originalSe.getDateExamen());
        		newSe.setDateFin(originalSe.getDateFin());
        	}else if(jours == 101){
        		newSe.setDateExamen(new Date());
        	}else {
	        	 Instant instant = originalSe.getDateExamen().toInstant().plus(jours, ChronoUnit.DAYS);
	             newSe.setDateExamen(Date.from(instant));
	             if(originalSe.getDateFin() != null) {
	            	 Instant instant2 = originalSe.getDateFin().toInstant().plus(jours, ChronoUnit.DAYS);
	            	 newSe.setDateFin(Date.from(instant2));
	             }else {
	            	 newSe.setDateFin(null);
	             }
        	}
        }else {
        	 newSe.setDateExamen(new Date());
        	 newSe.setDateFin(null);
        }
        newSe.setAnneeUniv(originalSe.getAnneeUniv());
        newSe.setCampus(originalSe.getCampus());
        newSe.setContext(context);
        newSe.setFinEpreuve(originalSe.getFinEpreuve());
        newSe.setHeureConvocation(originalSe.getHeureConvocation());
        newSe.setIsProcurationEnabled(originalSe.getIsProcurationEnabled());
        newSe.setComment(originalSe.getComment());
        newSe.setTypeBadgeage(originalSe.getTypeBadgeage());
        newSe.setIsSessionLibre(originalSe.isSessionLibre);
        newSe.setBlackListGroupe(originalSe.getBlackListGroupe());
        newSe.setIsSaveInExcluded(originalSe.getIsSaveInExcluded());
        newSe.setStatutSession(getStatutSession(newSe));
        int  x = 0 ;
        Long count = 0L;
        do {
          x++;
        } while (count!=0);
        if(!isSameName) {
        	String newNomEpreuve = originalSe.getNomSessionEpreuve() +  "(" + x + ")";
        	newSe.setNomSessionEpreuve(newNomEpreuve);
        }else {
        	newSe.setNomSessionEpreuve(originalSe.getNomSessionEpreuve());
        }
        newSe.setTypeSession(originalSe.getTypeSession());
        newSe.setHeureEpreuve(originalSe.getHeureEpreuve());
        newSe.setHeureConvocation(originalSe.getHeureConvocation());
        sessionEpreuveRepository.save(newSe);
        List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuve(originalSe);
        
        for(SessionLocation sl : sls) {
        	SessionLocation newSl = new SessionLocation();
        	newSl.setCapacite(sl.getCapacite());
        	newSl.setContext(context);
        	newSl.setIsTiersTempsOnly(sl.getIsTiersTempsOnly());
        	newSl.setLocation(sl.getLocation());
        	newSl.setPriorite(sl.getPriorite());
        	newSl.setSessionEpreuve(newSe);
        	sessionLocationRepository.save(newSl);
        	List<TagChecker> tcs = tagCheckerRepository.findBySessionLocation(sl);
        	for(TagChecker  tc : tcs) {
        		TagChecker newTc = new TagChecker();
        		newTc.setContext(context);
        		newTc.setSessionLocation(newSl);
        		newTc.setUserApp(tc.getUserApp());
        		tagCheckerRepository.save(newTc);
        	}
        }
        if(!originalSe.getIsSessionLibre()) {
        List<TagCheck> listTc  = tagCheckRepository.findTagCheckBySessionEpreuveId(originalSe.getId());
	        if(!listTc.isEmpty()) {
	        	for(TagCheck t : listTc) {
	        		TagCheck newTagCheck = new TagCheck();
	        		newTagCheck.setContext(context);
	        		newTagCheck.setIsTiersTemps(t.getIsTiersTemps());
	        		newTagCheck.setIsUnknown(t.getIsUnknown());
	        		newTagCheck.setNumAnonymat(t.getNumAnonymat());
	        		newTagCheck.setPerson(t.getPerson());
	        		newTagCheck.setGuest(t.getGuest());
	        		newTagCheck.setSessionEpreuve(newSe);
	        		Date endDate =  newSe.getDateFin() != null? newSe.getDateFin() : newSe.getDateExamen();
	        		List<Absence> absences = absenceRepository.findOverlappingAbsences(t.getPerson(),
	        				newSe.getDateExamen(), endDate);
					if(!absences.isEmpty()) {
						newTagCheck.setAbsence(absences.get(0));
	    			}
	        		tagCheckRepository.save(newTagCheck);
	        	}
	        }
        }
        boolean isOver = executeRepartition(newSe.getId(), "alpha");
       	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	log.info("Copie de la session : " + originalSe.getNomSessionEpreuve());
    	logService.log(ACTION.COPY_SESSION_EPREUVE, RETCODE.SUCCESS, originalSe.getNomSessionEpreuve() + " :: " + newSe.getNomSessionEpreuve(), auth.getName(), null, context.getKey(), null);

        return newSe;
	 }
	 
	 @Transactional
	 public void delete(SessionEpreuve se) {
		 Authentication auth = SecurityContextHolder.getContext().getAuthentication();		 
		 tagCheckService.deleteAllTagChecksBySessionEpreuveId(se.getId());
		 tagCheckerService.deleteAllTagCheckersBySessionEpreuveId(se.getId());
		 sessionLocationService.deleteAllTLocationsBySessionEpreuveId(se.getId());
		 esupSignatureService.deleteAllBySessionEpreuve(se);
		 storedFileService.deleteAllStoredFiles(se);
		 sessionEpreuveRepository.delete(se);
		 String eppnCible = auth !=null ? auth.getName() : "system";
		 logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.SUCCESS, se.getNomSessionEpreuve(), eppnCible, null, se.getContext().getKey(), null);
	 }
	 
	 @Transactional
	 public void deleteAll(List<SessionEpreuve> ses) {
		 for(SessionEpreuve se : ses) {
			 delete(se);
		 }
	 }
	 
	 public void addNbInscrits(List<SessionEpreuve> sessionEpreuveList) {
			for(SessionEpreuve session : sessionEpreuveList) {
				session.setNbInscritsSession(tagCheckRepository.countBySessionEpreuveId(session.getId()));
			}
	 }
	 
	 public boolean isSessionEpreuveClosed(SessionEpreuve se) {
		 if("CLOSED".equals(se.getStatutSession().getKey())) {
			 return true;
		 }
		return false;
	 }
	 
	 public HashMap<String,String> getTypesSession(Long ctxId){
		 
		 HashMap<String,String> map = new HashMap<>();
		 List<Object[]> list = sessionEpreuveRepository.findDistinctTypeSession(ctxId);
		 for(Object obj[] : list) {
			 map.put(obj[0].toString(), obj[1].toString());
		 }
		 return map;
	 }
	 
	 public Date setDateSessionEpreuve(String typeDate, String choice) {
		 Date date =null;
		 LocalDate now = LocalDate.now();
		 
		 switch(choice){
	       case "day": 
	    	   date = new Date();
	           break;
	   
	       case "week":
	    	   DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
	    	   if("dateDebut".equals(typeDate)) {
		           LocalDate startOfCurrentWeek = now.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
	    		   date = Date.from(startOfCurrentWeek.atStartOfDay(ZoneId.systemDefault()).toInstant());
	    	   }else {
	    		   DayOfWeek lastDayOfWeek = firstDayOfWeek.plus(6); // or minus(1)
		           LocalDate endOfWeek = now.with(TemporalAdjusters.nextOrSame(lastDayOfWeek));
		           date = Date.from(endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant());
	    	   }
	           break;
	   
	       case "month":
	    	   if("dateDebut".equals(typeDate)) {
	    		   LocalDate firstDay = now.with(TemporalAdjusters.firstDayOfMonth());
	    		   date = Date.from(firstDay.atStartOfDay(ZoneId.systemDefault()).toInstant());
	    	   }else {
	    		   LocalDate lastDay = now.with(TemporalAdjusters.lastDayOfMonth());
		           date = Date.from(lastDay.atStartOfDay(ZoneId.systemDefault()).toInstant());
	    	   }
	           break;
	   }
		 return date;
	 }

	public String importSessionsCsv(SequenceInputStream sequenceInputStream, String emargementContext) throws Exception {
		String bilanImport = "";
		String erreurs = "";
		int i = 0; int j=0;
		try (Reader reader = new InputStreamReader(sequenceInputStream, StandardCharsets.UTF_8);
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {
			for (CSVRecord record : csvParser) {
				boolean isRowOk = true;
				isRowOk = !record.get("nom").isEmpty() ? true : false;
				isRowOk = !record.get("type").isEmpty()
						&& !typeSessionRepository.findByKey(record.get("type")).isEmpty() ? true : false;
				isRowOk = !record.get("site").isEmpty()
						&& !campusRepository.findBySite(record.get("site")).isEmpty() ? true : false;
				isRowOk = !record.get("date_debut").isEmpty() ? true : false;
				isRowOk = !record.get("heure_debut").isEmpty() ? true : false;
				isRowOk = !record.get("heure_fin").isEmpty() ? true : false;

				if (isRowOk) {
					SessionEpreuve se = new SessionEpreuve();
					String nomSession = record.get("nom");
					Date dateExamen = new SimpleDateFormat("dd/MM/yy").parse(record.get("date_debut"));
					Date heureDebut = new SimpleDateFormat("HH:mm").parse(record.get("heure_debut"));
					Date heureFin = new SimpleDateFormat("HH:mm").parse(record.get("heure_fin"));
					if(sessionEpreuveRepository.countByNomSessionEpreuveAndDateExamenAndHeureEpreuveAndFinEpreuve(nomSession, dateExamen, 
							heureDebut, heureFin)>0) {
						erreurs +=  record.get("nom") + " ";
						j++;
					}else {
						se.setNomSessionEpreuve(nomSession);
						se.setTypeSession(typeSessionRepository.findByKey(record.get("type")).get(0));
						se.setCampus(campusRepository.findBySite(record.get("site")).get(0));
						se.setDateExamen(dateExamen);
						se.setDateCreation(new Date());
						se.setTypeBadgeage(TypeBadgeage.SESSION);
						se.setContext(contextService.getcurrentContext());
						if (!record.get("date_fin").isEmpty()) {
							se.setDateFin(new SimpleDateFormat("dd/MMM/yy").parse(record.get("date_fin")));
						}
						se.setHeureEpreuve(heureDebut);
						Calendar c = Calendar.getInstance();
						c.setTime(se.getHeureEpreuve());
						c.add(Calendar.MINUTE, -15);
						Date heureConvocation = c.getTime();
						se.setHeureConvocation(heureConvocation);
						se.setFinEpreuve(heureFin);
						if (!record.get("commentaire").isEmpty()) {
							se.setComment(record.get("commentaire"));
						}
						se.setStatutSession(getStatutSession(se));
						if (!record.get("session_libre").isEmpty()
								&& "O".equalsIgnoreCase(record.get("session_libre"))) {
							se.setIsSessionLibre(true);
						} else {
							se.setIsSessionLibre(false);
						}
						se.setAnneeUniv(String.valueOf(getCurrentAnneeUnivFromDate(se.getDateExamen())));
						sessionEpreuveRepository.save(se);
						i++;
					}
				}
			}
		} catch (Exception e) {
			log.error("CSV d'import de sessions non conforme", e);
			j++;
			erreurs = "Mauvais format de données dans le CSV";
			logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.FAILED, "Mauvais format de données" , null, null, emargementContext, null);
		}
		bilanImport = i + " importé(s) avec succès. - Erreur(s) : "  + j + " , "  + erreurs;
		log.info(bilanImport);
		logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.SUCCESS, "Import CSV : " + i , null, null, emargementContext, null);
		return bilanImport;
	}
	
	@Transactional
	public int updateStatutSession(String emargementcontext) {
		int nb= 0;
		int nbStatut = 7;
		List<StatutSession> list = statutSessionRepository.findByContextKey(emargementcontext);
		if(list.isEmpty()) {
			for(int i=0; i<nbStatut; i++) {
				StatutSession statutSession = new StatutSession();
				Context ctx = contextRepository.findByKey(emargementcontext);
				statutSession.setContext(ctx);
				statutSession.setLibelle(messageSource.getMessage("statutSession." + i + ".libelle", null, null));
				statutSession.setColor(messageSource.getMessage("statutSession." + i + ".color", null, null));
				statutSession.setDescription(messageSource.getMessage("statutSession." + i + ".description", null, null));
				statutSession.setIsClickable(Boolean.valueOf(messageSource.getMessage("statutSession." + i + ".isClickable", null, null)));
				statutSession.setIsDefault(Boolean.valueOf(messageSource.getMessage("statutSession." + i + ".isDefault", null, null)));
				statutSession.setIsVisible(Boolean.valueOf(messageSource.getMessage("statutSession." + i + ".isVisible", null, null)));
				statutSession.setOrdre(Integer.valueOf(messageSource.getMessage("statutSession." + i + ".ordre", null, null)));
				statutSession.setKey(messageSource.getMessage("statutSession." + i + ".key", null, null));
				statutSessionRepository.save(statutSession);
				nb++;
			}
			log.info("Ajout de status de session : " + nb );
			logService.log(ACTION.CREATE_STATUTS_SESSION, RETCODE.SUCCESS, "Ajout de statuts de session : " + nb, null,  null, "all", null);
		}
		return nb;
	}
	
	public StatutSession getStatutSession(SessionEpreuve se) {
	    LocalDate today = LocalDate.now();
	    Date dateExamenDate = se.getDateExamen();
	    Date dateFinDate = se.getDateFin();
	    Context ctx = se.getContext();
	    LocalDate dateExamen = dateExamenDate.toInstant()
	        .atZone(ZoneId.systemDefault())
	        .toLocalDate();
	    if (dateFinDate != null) {
	        LocalDate dateFin = dateFinDate.toInstant()
	            .atZone(ZoneId.systemDefault())
	            .toLocalDate();
	        if ((today.isEqual(dateExamen) || today.isAfter(dateExamen)) &&
	            (today.isEqual(dateFin) || today.isBefore(dateFin))) {
	            return statutSessionRepository.findByKeyAndContext("OPENED", ctx);
	        }
			return statutSessionRepository.findByKeyAndContext("STANDBY", ctx);
	    }
		if (today.isEqual(dateExamen)) {
		    return statutSessionRepository.findByKeyAndContext("OPENED", ctx);
		} else if (today.isAfter(dateExamen)) {
		    return statutSessionRepository.findByKeyAndContext("ENDED", ctx);
		} else {
		    return statutSessionRepository.findByKeyAndContext("STANDBY", ctx);
		}
	}
	
	@Transactional
	public void migrateAllStatutSession(Context context) {
		log.info("Migration des statuts de session : ");
		List<SessionEpreuve> ses = sessionEpreuveRepository.findSessionEpreuveByContext(context);
		if(!ses.isEmpty()) {
			for(SessionEpreuve se : ses) {
				// OPENED, STANDBY, CLOSED, CANCELLED
				log.info("Maj statut session : " + se.getNomSessionEpreuve());
				if(se.getStatutSession()==null) {
					Statut statut = se.getStatut()!=null ? se.getStatut() : Statut.STANDBY;
					StatutSession  statutSession  =  statutSessionRepository.findByKeyAndContext(statut.name(), context);
					log.info(statutSession.getKey());
					se.setStatutSession(statutSession);
					sessionEpreuveRepository.save(se);
					log.info("Maj statut Session pour " + se.getNomSessionEpreuve());
				}else {
					log.info("Aucune Maj statut session pour : " + se.getNomSessionEpreuve());
				}
			}	
		}else {
			log.info("RAS");
		}
	}
}
