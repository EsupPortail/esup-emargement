package org.esupportail.emargement.services;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Plan;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.PropertiesForm;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
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
	private CampusRepository campusRepository;
	
	@Autowired
	private SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	PrefsRepository prefsRepository;
	
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
	LocationService locationService;
	
	@Resource
	AppliConfigService appliConfigService;

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
    	boolean isOver =false;
    	if(tagCheckOrder==null ) {
    		tagCheckOrder = "alpha";
		}
    	//Tiers-temps
    	Long countTagChecksIsTiersTemps = tagCheckService.countNbTagCheckRepartitionNull(sessionEpreuveId, true);
    	Long countTagChecksRepartisIsTiersTemps = tagCheckService.countNbTagCheckRepartitionNotNull(sessionEpreuveId, true);
    	int capaciteTotaleIsTiersTemps= countCapaciteTotalSessionLocations(sessionEpreuveId, true);
    	Long capaciteRestanteIsTiersTemps = Long.valueOf(capaciteTotaleIsTiersTemps) - countTagChecksRepartisIsTiersTemps;
    	
    	//Non Tiers-temps
    	Long countTagChecksIsNotTiersTemps = tagCheckService.countNbTagCheckRepartitionNull(sessionEpreuveId, false);
    	Long countTagChecksRepartisIsNotTiersTemps = tagCheckService.countNbTagCheckRepartitionNotNull(sessionEpreuveId, false);
    	int capaciteTotaleIsNotTiersTemps= countCapaciteTotalSessionLocations(sessionEpreuveId, false);
    	Long capaciteRestanteIsNotTiersTemps = Long.valueOf(capaciteTotaleIsNotTiersTemps) - countTagChecksRepartisIsNotTiersTemps;
    	
    	if(countTagChecksIsTiersTemps<=capaciteRestanteIsTiersTemps && countTagChecksIsNotTiersTemps<=capaciteRestanteIsNotTiersTemps) {
    		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(sessionEpreuveId).get();
    		int j=0;
    		//Tiers-temps
    		List<SessionLocation> sessionLocationListIsTiersTempsOnly= sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyTrueOrderByPriorite(sessionEpreuveId);
			for(SessionLocation sl:  sessionLocationListIsTiersTempsOnly) {
				//on compte le nombre le nombre de place utilisé dans cette salle
				Long nbUsedPlace = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrueAndSessionLocationExpectedId(sessionEpreuveId, sl.getId());
				List<TagCheck> tagCheckList = null;
				if("alpha".equals(tagCheckOrder)) {
					tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrueOrderByPersonEppn(sessionEpreuveId);
					tagCheckService.setNomPrenomTagChecks(tagCheckList, false, false);
					List<TagCheck> sortedUsers = tagCheckList.stream()
							  .sorted(Comparator.comparing(TagCheck::getNomPrenom))
							  .collect(Collectors.toList());
					tagCheckList = sortedUsers;
				}else if("numEtu".equals(tagCheckOrder)) {
					tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrueOrderByPersonNumIdentifiant(sessionEpreuveId);
				}
				else {
					tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(sessionEpreuveId);
				}
				if(!tagCheckList.isEmpty()) {
					if(nbUsedPlace.intValue()<sl.getCapacite()) {
		    			for(TagCheck tc:  tagCheckList) {
    	    				if(tc.getIsUnknown()) {
    	    					tagCheckRepository.delete(tc);
    	    				}else {
			    				if(nbUsedPlace.intValue()<sl.getCapacite()) {
			    					tc.setSessionLocationExpected(sl);
			    					tc.setNumAnonymat(constructNumAnonymat(sessionEpreuve, j));
			    					tagCheckRepository.save(tc);
			    					nbUsedPlace++;
			    					j++;
			    				}
    	    				}
		    			}
					}
	    		}else {
	    			break;
	    		}
			}
			
    		//Non Tiers-temps
    		List<SessionLocation> sessionLocationListIsNotTiersTempsOnly= sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyFalseOrderByPriorite(sessionEpreuveId);
			for(SessionLocation sl:  sessionLocationListIsNotTiersTempsOnly) {
				List<TagCheck> tagCheckList = null;
				if("alpha".equals(tagCheckOrder)) {
					tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalseOrderByPersonEppn(sessionEpreuveId);
					tagCheckService.setNomPrenomTagChecks(tagCheckList, false, false);
					List<TagCheck> sortedUsers = tagCheckList.stream()
							  .sorted(Comparator.comparing(TagCheck::getNomPrenom))
							  .collect(Collectors.toList());
					tagCheckList = sortedUsers;
				}else if("numEtu".equals(tagCheckOrder)) {
					tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalseOrderByPersonNumIdentifiant(sessionEpreuveId);
				}else{
					tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(sessionEpreuveId);
				}
				//on compte le nombre le nombre de place utilisé dans cette salle
				Long nbUsedPlace = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalseAndSessionLocationExpectedId(sessionEpreuveId, sl.getId());
				
				// Pour le plan
				List<TagCheck> checks = tagCheckOrder.compareTo("1")==0 ? 
						tagCheckRepository.findByIsTiersTempsFalseAndSessionEpreuve_IdAndSessionLocationExpectedIsNullOrderByPerson_Id(sessionEpreuveId) 
						: tagCheckRepository.findByIsTiersTempsFalseAndSessionEpreuve_IdAndSessionLocationExpectedIsNull(sessionEpreuveId);
				
				System.out.println("checks -----------------> " + checks);
				if(!checks.isEmpty()) {

					short i = 0;
	
	                Location location = sl.getLocation();
	                Plan plan = locationService.findPlan(location.getId());
	                System.out.println("plan -----------------> " + plan);
	                if(plan != null) {
	                        short planCols = plan.getColumns();
	                        Short[] places = plan.getStandardPlaces();
	                        while(nbUsedPlace < sl.getCapacite() && i < checks.size()) {
	                                TagCheck check = checks.get(i);
	                                if(check.getIsUnknown()) tagCheckRepository.delete(check);
	                                else {
	                                        Short place = places[i];
	                                        check.setPlace(plan.getHasAlphanumEnum() ? String.format("%c%d", 'A' + Math.floorDiv(place - 1, planCols), 1 + (place - 1) % planCols) : String.valueOf(place));
	                                        check.setSessionLocationExpected(sl);
	                                        check.setNumAnonymat(constructNumAnonymat(sessionEpreuve, j));
	                                        tagCheckRepository.save(check);
	                                        nbUsedPlace++;
	                                        j++;
	                                        i++;
	                                }

	                				System.out.println("check -----------------> " + check);
	                        }
	                }
                }

				if(!tagCheckList.isEmpty()) {
    				if(nbUsedPlace.intValue()<sl.getCapacite()) {
    	    			for(TagCheck tc:  tagCheckList) {
    	    				
    	    				if(tc.getIsUnknown()) {
    	    					tagCheckRepository.delete(tc);
    	    				}else {
	    	    				if(nbUsedPlace.intValue()<sl.getCapacite()) {
		        					tc.setSessionLocationExpected(sl);
		        					tc.setNumAnonymat(constructNumAnonymat(sessionEpreuve, j));
		        					tagCheckRepository.save(tc);
		        					nbUsedPlace++;
		        					j++;
	    	    				}
    	    				}
    	    			}	
    				}
	    		}else {
	    			break;
	    		}
			}
    	}else {
    		isOver = true;
    	}
    	return isOver;
    }
    
    public void affinageRepartition(PropertiesForm propertiesForm, String emargementContext, String tagCheckOrder){
    	if(tagCheckOrder==null ) {
    		tagCheckOrder = "alpha";
		}
    	List <SessionLocation> list = propertiesForm.list;
    	if(!list.isEmpty()) {
    		SessionEpreuve se = list.get(0).getSessionEpreuve();
    		tagCheckService.resetSessionLocationExpected(se.getId());
    		List <SessionLocation> isTiersTempsOnlyList = new ArrayList<SessionLocation>();
    		List <SessionLocation> notTiersTempsList = new ArrayList<SessionLocation>();
    		for(SessionLocation sl : list) {
    			if(sl.getIsTiersTempsOnly()) {
    				isTiersTempsOnlyList.add(sl);
    			}else {
    				notTiersTempsList.add(sl);
    			}
    		}
    		//Tiers-temps
    		List<TagCheck> tagCheckIsTiersTempsOnlyList = null;
			if("alpha".equals(tagCheckOrder)){
				tagCheckIsTiersTempsOnlyList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonEppn(se.getId());
				tagCheckService.setNomPrenomTagChecks(tagCheckIsTiersTempsOnlyList, false, false);
				List<TagCheck> sortedUsers = tagCheckIsTiersTempsOnlyList.stream()
						  .sorted(Comparator.comparing(TagCheck::getNomPrenom))
						  .collect(Collectors.toList());
				tagCheckIsTiersTempsOnlyList = sortedUsers;
			}else if("numEtu".equals(tagCheckOrder)) {
				tagCheckIsTiersTempsOnlyList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonNumIdentifiant(se.getId());
			}else{
				tagCheckIsTiersTempsOnlyList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(se.getId());
			}
    		int i = 0;
    		int j = 0;
    		SessionLocation sl = null;
    		if(!tagCheckIsTiersTempsOnlyList.isEmpty() && !isTiersTempsOnlyList.isEmpty()) {
    			sl = isTiersTempsOnlyList.get(j);
	    		for(TagCheck tc : tagCheckIsTiersTempsOnlyList) {
					if(i<sl.getNbInscritsSessionLocation() && j < isTiersTempsOnlyList.size() && i < sl.getCapacite()) {
						tc.setSessionLocationExpected(sl);
						tagCheckRepository.save(tc);
						i++;
					}else {
						i=1;
						j++;
						sl = isTiersTempsOnlyList.get(j);
						tc.setSessionLocationExpected(sl);
						tagCheckRepository.save(tc);
					}
	    		}
    		}
    		//Non Tiers-temps
    		List<TagCheck> tagCheckNotTiersTempsList = null;
    		if("alpha".equals(tagCheckOrder)){
				tagCheckNotTiersTempsList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonEppn(se.getId());
				tagCheckService.setNomPrenomTagChecks(tagCheckNotTiersTempsList, false, false);
				List<TagCheck> sortedUsers = tagCheckNotTiersTempsList.stream()
						  .sorted(Comparator.comparing(TagCheck::getNomPrenom))
						  .collect(Collectors.toList());
				tagCheckNotTiersTempsList = sortedUsers;
			}else if("numEtu".equals(tagCheckOrder)) {
				tagCheckNotTiersTempsList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonNumIdentifiant(se.getId());
			}else{
				tagCheckNotTiersTempsList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(se.getId());
			}
    		i = 0;
    		j = 0;
    		if(!tagCheckNotTiersTempsList.isEmpty() && !notTiersTempsList.isEmpty()) {
    			sl = notTiersTempsList.get(j);
				for(TagCheck tc : tagCheckNotTiersTempsList) {
					if(i<sl.getNbInscritsSessionLocation() && j < notTiersTempsList.size() && i < sl.getCapacite()) {
						tc.setSessionLocationExpected(sl);
						tagCheckRepository.save(tc);
						i++;
					}else {
						
						i=1;
						j++;
						sl = notTiersTempsList.get(j);
						tc.setSessionLocationExpected(sl);
						tagCheckRepository.save(tc);
					}
				}
    		}
			logService.log(ACTION.AFFINER_REPARTITION, RETCODE.SUCCESS, "Session : " + se.getNomSessionEpreuve() , null, null, emargementContext, null);
			log.info("affinage session ; " + se.getNomSessionEpreuve());
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
	public void save(SessionEpreuve sessionEpreuve, String emargementContext) throws IOException {
		
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
    	nomFichier = nomFichier.replace(" ", "_");		
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
	        		String signature = tc.getAbsence()!=null? tc.getAbsence().name() : "";
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
	
	@Scheduled(cron = "0 0 7 * * ?")//Tous les jours à 10h
	public void closeSessions(){
		List<AppliConfig> configs =  appliConfigRepository.findAppliConfigByKey(AppliConfigKey.AUTO_CLOSE_SESSION.name());
		for(AppliConfig config : configs) {
			if("true".equals(config.getValue())) {
				List<SessionEpreuve> list = sessionEpreuveRepository.findSessionEpreuveByContext(config.getContext());
				if(!list.isEmpty()) {
					for(SessionEpreuve se : list) {
						int check = toolUtil.compareDate(se.getDateExamen(), new Date(), "yyyy-MM-dd");
						int checkIfDateFinIsOk = -1;
						if(se.getDateFin() != null) {
							checkIfDateFinIsOk = toolUtil.compareDate(se.getDateFin(), new Date(), "yyyy-MM-dd");
						}else {
							checkIfDateFinIsOk = check;
						}
						if(check<0 && checkIfDateFinIsOk < 0) {
							se.setStatut(Statut.CLOSED);
							sessionEpreuveRepository.save(se);
							logService.log(ACTION.CLOSE_SESSION, RETCODE.SUCCESS, "Session : " + se.getNomSessionEpreuve() , null, null, se.getContext().getKey(), null);
							log.info("cloture utomatique de session " + se.getNomSessionEpreuve());
						}
					}
				}
			}
		}
	}
	
	public List<SessionEpreuve> getListSessionEpreuveByTagchecker(String eppn, String nom){
		
		Page<TagChecker> tagCheckerList = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppn, null);
		List<Prefs> prefs = prefsRepository.findByUserAppEppnAndNom(eppn, nom);
		Prefs pref = null;
		if(!prefs.isEmpty()) {
			pref = prefs.get(0);
		}
		List<SessionEpreuve> newList = new ArrayList<>();
		for (TagChecker tc :  tagCheckerList.getContent()) {
			SessionEpreuve se = tc.getSessionLocation().getSessionEpreuve();
			if(!newList.contains(se) && !Statut.CLOSED.equals(se.getStatut()) && !Statut.CANCELLED.equals(se.getStatut())) {
				if(pref != null && "false".equals(pref.getValue()) || pref == null) {
					int check = toolUtil.compareDate(se.getDateExamen(), new Date(), "yyyy-MM-dd");
					int checkIfDateFinIsOk = -1;
					if(se.getDateFin() != null) {
						checkIfDateFinIsOk = toolUtil.compareDate(se.getDateFin(), new Date(), "yyyy-MM-dd");
					}else {
						checkIfDateFinIsOk = check;
					}
					if(check>=0 || checkIfDateFinIsOk>=0) {
						newList.add(se);
					}
				}else {
					newList.add(se);
				}
			}
		}
		return newList;
	}
	
	public List<String> getYears(String ctx) {
		
		List<String> years = new ArrayList<>();

		Context context = contextRepository.findByContextKey(ctx);
		List<String> anneeUnivs = null;
		if("all".equals(ctx)) {
			anneeUnivs = sessionEpreuveRepository.findDistinctAnneeUnivAll();
		}else {
			 anneeUnivs = sessionEpreuveRepository.findDistinctAnneeUniv(context.getId());
		}
		
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		int start = year;
		if(month<8) {
			start = year-1;
		}
		int end = start +1;
		if(!anneeUnivs.isEmpty()) {
			start = Integer.valueOf(anneeUnivs.get(0));
			if(month<8) {
				end = year;
			}else {
				end = year + 1;
			}
		}
		
		for(int i= start;i<end; i++) {
			years.add(String.valueOf(i) + "/" + String.valueOf(i + 1));
		}
		return years;
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
	
	 public SessionEpreuve duplicateSessionEpreuve(Long id){
		SessionEpreuve originalSe = sessionEpreuveRepository.findById(id).get();
		Context context = originalSe.getContext();
        SessionEpreuve newSe = new SessionEpreuve();
        newSe.setAnneeUniv(originalSe.getAnneeUniv());
        newSe.setCampus(originalSe.getCampus());
        newSe.setContext(context);
        newSe.setDateExamen(new Date());
        newSe.setFinEpreuve(originalSe.getFinEpreuve());
        newSe.setHeureConvocation(originalSe.getHeureConvocation());
        newSe.setStatut(null);
        newSe.setIsProcurationEnabled(originalSe.getIsProcurationEnabled());
        newSe.setComment(originalSe.getComment());
        newSe.setTypeBadgeage(originalSe.getTypeBadgeage());
        newSe.setIsSessionLibre(originalSe.isSessionLibre);
        newSe.setBlackListGroupe(originalSe.getBlackListGroupe());
        newSe.setIsSaveInExcluded(originalSe.getIsSaveInExcluded());
        newSe.setStatut(originalSe.getStatut());
        String newNomEpreuve = "";
        int  x = 0 ;
        Long count = 0L;
        do {
          x++;
        } while (count!=0);
        newNomEpreuve = originalSe.getNomSessionEpreuve() +  "(" + x + ")";
        newSe.setNomSessionEpreuve(newNomEpreuve);
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
	        		tagCheckRepository.save(newTagCheck);
	        	}
	        }
        }
        
       	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	log.info("Cpoie de la session : " + originalSe.getNomSessionEpreuve());
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
		 logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.SUCCESS, se.getNomSessionEpreuve(), auth.getName(), null, se.getContext().getKey(), null);
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
		 if(Statut.CLOSED.equals(se.getStatut())) {
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
						if (!record.get("statut").isEmpty() && "O".equalsIgnoreCase(record.get("statut"))) {
							se.setStatut(Statut.OPENED);
						} else {
							se.setStatut(Statut.STANDBY);
						}
						if (!record.get("session_libre").isEmpty()
								&& "O".equalsIgnoreCase(record.get("session_libre"))) {
							se.setIsSessionLibre(true);
						} else {
							se.setIsSessionLibre(false);
						}
						se.setAnneeUniv(String.valueOf(getCurrentanneUniv()));
						sessionEpreuveRepository.save(se);
						i++;
					}
				}
			}
		} catch (Exception e) {
			log.error("CSV d'imort de sesions non conforme", e);
			j++;
			erreurs = "Mauvais format de données dans le CSV";
			logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.FAILED, "Mauvais format de données" , null, null, emargementContext, null);
		}
		bilanImport = i + " importé(s) avec succès. - Erreur(s) : "  + j + " , "  + erreurs;
		log.info(bilanImport);
		logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.SUCCESS, "Import CSV : " + i , null, null, emargementContext, null);
		return bilanImport;
	}
}
