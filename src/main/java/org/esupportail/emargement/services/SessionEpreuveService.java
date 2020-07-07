package org.esupportail.emargement.services;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.PropertiesForm;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeSessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.BigFileRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.services.AppliConfigService.AppliConfigKey;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class SessionEpreuveService {
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	private SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	private AppliConfigRepository appliConfigRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Autowired
	BigFileRepository bigFileRepository;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Resource
	LdapService ldapService;
		
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	StoredFileService storedFileService;
	
	@Resource
	LogService logService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Autowired
	ToolUtil toolUtil;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void computeCounters(List<SessionEpreuve> sessionEpreuveList) {

		for(SessionEpreuve session : sessionEpreuveList) {
			session.setNbLieuxSession(sessionLocationRepository.countBySessionEpreuveId(session.getId()));
			List<SessionLocation> locations = sessionLocationRepository.findSessionLocationBySessionEpreuve(session);
			Long nbTagCheckerSession = Long.valueOf("0");
			for(SessionLocation location : locations) {
				 nbTagCheckerSession += tagCheckerRepository.countBySessionLocationId(location.getId());
			}
			Long countDispatch = tagCheckService.countNbTagCheckRepartitionNotNull(session.getId(), true) + tagCheckService.countNbTagCheckRepartitionNotNull(session.getId(), false);
			session.setNbDispatchTagCheck(countDispatch);
			session.setNbPresentsSession(tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNullAndSessionLocationExpectedIsNotNull(session.getId()));
			session.setNbTagCheckerSession(nbTagCheckerSession);
			Long unknown = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNotNull(session.getId());
			session.setNbInscritsSession(tagCheckRepository.countBySessionEpreuveId(session.getId())-unknown);
			session.setDureeEpreuve(getDureeEpreuve(session));
			session.setNbCheckedByCardTagCheck(tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsCheckedByCardTrue(session.getId()));
			session.setNbUnknown(unknown);
		}
	}

    public boolean executeRepartition(Long sessionEpreuveId) {
    	boolean isOver =false;
    	//Tiers-temps
    	Long countTagChecksIsTiersTemps = tagCheckService.countNbTagCheckRepartitionNull(sessionEpreuveId, true);
    	Long countTagChecksRepartisIsTiersTemps = tagCheckService.countNbTagCheckRepartitionNotNull(sessionEpreuveId, true);
    	int capaciteTotaleIsTiersTemps= sessionEpreuveService.countCapaciteTotalSessionLocations(sessionEpreuveId, true);
    	Long capaciteRestanteIsTiersTemps = new Long(capaciteTotaleIsTiersTemps) - countTagChecksRepartisIsTiersTemps;
    	
    	//Non Tiers-temps
    	Long countTagChecksIsNotTiersTemps = tagCheckService.countNbTagCheckRepartitionNull(sessionEpreuveId, false);
    	Long countTagChecksRepartisIsNotTiersTemps = tagCheckService.countNbTagCheckRepartitionNotNull(sessionEpreuveId, false);
    	int capaciteTotaleIsNotTiersTemps= sessionEpreuveService.countCapaciteTotalSessionLocations(sessionEpreuveId, false);
    	Long capaciteRestanteIsNotTiersTemps = new Long(capaciteTotaleIsNotTiersTemps) - countTagChecksRepartisIsNotTiersTemps;
    	
    	if(countTagChecksIsTiersTemps<=capaciteRestanteIsTiersTemps && countTagChecksIsNotTiersTemps<=capaciteRestanteIsNotTiersTemps) {
    		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(sessionEpreuveId).get();
    		int j=0;
    		//Tiers-temps
    		List<SessionLocation> sessionLocationListIsTiersTempsOnly= sessionLocationRepository.findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyTrueOrderByPriorite(sessionEpreuveId);
			for(SessionLocation sl:  sessionLocationListIsTiersTempsOnly) {
				//on compte le nombre le nombre de place utilisé dans cette salle
				Long nbUsedPlace = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrueAndSessionLocationExpectedId(sessionEpreuveId, sl.getId());
				List<TagCheck> tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(sessionEpreuveId);
				if(!tagCheckList.isEmpty()) {
					if(nbUsedPlace.intValue()<sl.getCapacite()) {
		    			for(TagCheck tc:  tagCheckList) {
    	    				if(tc.getIsUnknown()) {
    	    					tagCheckRepository.delete(tc);
    	    				}else {
			    				if(nbUsedPlace.intValue()<sl.getCapacite()) {
			    					tc.setSessionLocationExpected(sl);
			    					tc.setNumAnonymat(sessionEpreuveService.constructNumAnonymat(sessionEpreuve, j));
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
				List<TagCheck> tagCheckList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(sessionEpreuveId);
				//on compte le nombre le nombre de place utilisé dans cette salle
				Long nbUsedPlace = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalseAndSessionLocationExpectedId(sessionEpreuveId, sl.getId());
				if(!tagCheckList.isEmpty()) {
    				if(nbUsedPlace.intValue()<sl.getCapacite()) {
    	    			for(TagCheck tc:  tagCheckList) {
    	    				if(tc.getIsUnknown()) {
    	    					tagCheckRepository.delete(tc);
    	    				}else {
	    	    				if(nbUsedPlace.intValue()<sl.getCapacite()) {
		        					tc.setSessionLocationExpected(sl);
		        					tc.setNumAnonymat(sessionEpreuveService.constructNumAnonymat(sessionEpreuve, j));
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
    
    
    public void affinageRepartition(PropertiesForm propertiesForm, String emargementContext){
    	
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
    		List<TagCheck> tagCheckIsTiersTempsOnlyList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(se.getId());
    		int i = 0;
    		int j = 0;
    		SessionLocation sl = null;
    		if(!tagCheckIsTiersTempsOnlyList.isEmpty() && !isTiersTempsOnlyList.isEmpty()) {
    			sl = isTiersTempsOnlyList.get(j);
	    		for(TagCheck tc : tagCheckIsTiersTempsOnlyList) {
					if(i<sl.nbInscritsSessionLocation && j < isTiersTempsOnlyList.size() && i < sl.getCapacite()) {
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
    		List<TagCheck> tagCheckNotTiersTempsList = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(se.getId());
    		i = 0;
    		j = 0;
    		if(!tagCheckNotTiersTempsList.isEmpty() && !notTiersTempsList.isEmpty()) {
    			sl = notTiersTempsList.get(j);
				for(TagCheck tc : tagCheckNotTiersTempsList) {
					if(i<sl.nbInscritsSessionLocation && j < notTiersTempsList.size() && i < sl.getCapacite()) {
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
	
	public void save(SessionEpreuve sessionEpreuve, String emargementContext) throws IOException {
		
		StoredFile sf = null;
		StoredFile oldSf =  null;
		boolean deleteOldPlan =false;
		if(sessionEpreuve.getId()!=null){
			Optional<SessionEpreuve> se = sessionEpreuveRepository.findById(sessionEpreuve.getId());
			oldSf = se.get().getPlanSessionEpreuve();
		}
		if(sessionEpreuve.getFile() != null && sessionEpreuve.getFile().getSize()>0) {
			  sf = storedFileService.setStoredFile(new StoredFile(), sessionEpreuve.getFile(), emargementContext);
			  deleteOldPlan = true;
		}else {
			sf = oldSf;
		}
		
		sessionEpreuve.setPlanSessionEpreuve(sf);
		sessionEpreuveRepository.save(sessionEpreuve);
		if(deleteOldPlan) {
			if(oldSf!=null) {
				storedFileRepository.delete(oldSf);
			}
		}
	}
	
	
	
	public void exportEmargement(HttpServletResponse response,  Long sessionLocationId, Long sessionEpreuveId, String type, String emargementContext) {
		
		List<TagCheck> list = tagCheckRepository.findTagCheckBySessionLocationExpectedId(sessionLocationId);
		SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
		SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
    	String nomFichier = "Liste_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()));
    	nomFichier = nomFichier.replace(" ", "_");		
		tagCheckService.setNomPrenomTagChecks(list);
		Collections.sort(list,  new Comparator<TagCheck>() {	
			
			@Override
            public int compare(TagCheck obj1, TagCheck obj2) {
				if(obj1.getPerson().getNom() !=null) {
					return obj1.getPerson().getNom().compareTo(obj2.getPerson().getNom());
				}else {
					return obj1.getPerson().getEppn().compareTo(obj2.getPerson().getEppn());
				}
			}
		});
		PdfPTable table = null;
		if("Liste".equals(type)) {
			int nbColumn = (se.getIsProcurationEnabled())? 7 : 6;
	    	table = new PdfPTable(nbColumn);
	    	
	    	table.setWidthPercentage(100);
	    	table.setHorizontalAlignment(Element.ALIGN_CENTER);
	    	
	        //On créer l'objet cellule.
	        String libelleSe = list.get(0).getSessionLocationExpected().getSessionEpreuve().getNomSessionEpreuve().concat(" ").
	        		concat(String.format("%1$td-%1$tm-%1$tY", (list.get(0).getSessionLocationExpected().getSessionEpreuve().getDateExamen())).
	        		concat(" à ").concat(list.get(0).getSessionLocationExpected().getLocation().getCampus().getSite()).concat(" --  ").
	        		concat(list.get(0).getSessionLocationExpected().getLocation().getNom()));
	        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
	        cell.setBackgroundColor(BaseColor.GREEN);
	        cell.setColspan(nbColumn);
	        table.addCell(cell);
	   
	        //contenu du tableau.
	        PdfPCell header1 = new PdfPCell(new Phrase("Nom")); header1.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header2 = new PdfPCell(new Phrase("Prénom")); header2.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header3 = new PdfPCell(new Phrase("Eppn")); header3.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header4 = new PdfPCell(new Phrase("Numéro identifiant")); header4.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header5 = new PdfPCell(new Phrase("Tiers-temps")); header5.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header6 = new PdfPCell(new Phrase("Procuration")); header6.setBackgroundColor(BaseColor.GRAY);
	        PdfPCell header7 = new PdfPCell(new Phrase("Signature")); header7.setBackgroundColor(BaseColor.GRAY);
	        
	        table.addCell(header1);
	        table.addCell(header2);
	        table.addCell(header3);
	        table.addCell(header4);
	        table.addCell(header5);
	        if(se.getIsProcurationEnabled()) {
	        	table.addCell(header6);
	        }
	        table.addCell(header7);
	        
	        if(!list.isEmpty()) {
	        	for(TagCheck tc : list) {
	        		PdfPCell cell1 = new PdfPCell(new Phrase(tc.getPerson().getNom())); cell1.setMinimumHeight(30); 
	        		PdfPCell cell2 = new PdfPCell(new Phrase(tc.getPerson().getPrenom())); cell2.setMinimumHeight(30);
	        		PdfPCell cell3 = new PdfPCell(new Phrase(tc.getPerson().getEppn())); cell3.setMinimumHeight(30);
	        		PdfPCell cell4 = new PdfPCell(new Phrase(tc.getPerson().getNumIdentifiant())); cell4.setMinimumHeight(30);
	        		PdfPCell cell5 = new PdfPCell(new Phrase((tc.getIsTiersTemps())? "Oui": "Non")); cell5.setMinimumHeight(30);
	        		PdfPCell cell6 = new PdfPCell(new Phrase((tc.getProxyPerson()!=null)? tc.getProxyPerson().getNom(): "")); cell6.setMinimumHeight(30);
	        		PdfPCell cell7 = new PdfPCell(new Phrase("")); cell7.setMinimumHeight(30);
	        		
	        		
	    	        table.addCell(cell1);
	    	        table.addCell(cell2);
	    	        table.addCell(cell3);
	    	        table.addCell(cell4);
	    	        table.addCell(cell5);
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
	public void closeSessions() throws ParseException {
		List<AppliConfig> configs =  appliConfigRepository.findAppliConfigByKey(AppliConfigKey.AUTO_CLOSE_SESSION.name());
		for(AppliConfig config : configs) {
			if("true".equals(config.getValue())) {
				List<SessionEpreuve> list = sessionEpreuveRepository.findSessionEpreuveByContext(config.getContext());
				if(!list.isEmpty()) {
					for(SessionEpreuve se : list) {
						int check = toolUtil.compareDate(se.getDateExamen(), new Date(), "yyyy-MM-dd");
						if(check<0) {
							se.setIsSessionEpreuveClosed(true);
							sessionEpreuveRepository.save(se);
							logService.log(ACTION.CLOSE_SESSION, RETCODE.SUCCESS, "Session : " + se.getNomSessionEpreuve() , null, null, se.getContext().getKey(), null);
							log.info("cloture utomatique de session " + se.getNomSessionEpreuve());
						}
					}
				}
			}
		}
	}
	
	public String getDureeEpreuve(SessionEpreuve se) {
		
		String duree ="";
		
		Date heureEpreuve = se.getHeureEpreuve();
		Date finEpreuve = se.getFinEpreuve();
		//Date dureeEpreuve = sameSe.getDureeEpreuve();
		long diff = finEpreuve.getTime() - heureEpreuve.getTime();
		
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		if(diffHours != 0) {
			duree = String.valueOf(diffHours).concat("H");
		}
		if(diffMinutes != 0) {
			duree = duree.concat(StringUtils.leftPad(String.valueOf(diffMinutes), 2, "0"));
			if(diffHours == 0) {
				duree = duree.concat("mn");
			}
		}
		
		return duree;
	}
	
	public List<SessionEpreuve> getListSessionEpreuveByTagchecker(String eppn){
		
		Page<TagChecker> tagCheckerList = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppn, null);
		List<SessionEpreuve> newList = new ArrayList<SessionEpreuve>();
		for (TagChecker tc :  tagCheckerList.getContent()) {
			SessionEpreuve se = tc.getSessionLocation().getSessionEpreuve();
			if(!newList.contains(se) && !se.getIsSessionEpreuveClosed()) {
				newList.add(tc.getSessionLocation().getSessionEpreuve());
			}
		}
		return newList;
	}
	
	public List<String> getYears() {

		List<String> years = new ArrayList<String>();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		int start = year;
		int end = start +1;
		if(month<8) {
			start = year-1;
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
	public List<String> getListTypeSessionEpreuve() {
		List<TypeSessionEpreuve> enumTypes = new ArrayList<TypeSessionEpreuve>(Arrays.asList(TypeSessionEpreuve.values()));
		List<String> types = new ArrayList<String>();
		for(TypeSessionEpreuve t: enumTypes) {
			types.add(t.name());
		}
		return types;
	}
	
	 public SessionEpreuve duplicateSessionEpreuve(Long id) throws IOException {
		SessionEpreuve originalSe = sessionEpreuveRepository.findById(id).get();
		Context context = originalSe.getContext();
        SessionEpreuve newSe = new SessionEpreuve();
        newSe.setAnneeUniv(originalSe.getAnneeUniv());
        newSe.setCampus(originalSe.getCampus());
        newSe.setContext(context);
        newSe.setDateExamen(new Date());
        newSe.setFile(originalSe.getFile());
        newSe.setFinEpreuve(originalSe.getFinEpreuve());
        newSe.setHeureConvocation(originalSe.getHeureConvocation());
        newSe.setIsSessionEpreuveClosed(false);
        newSe.setIsProcurationEnabled(originalSe.getIsProcurationEnabled());
        newSe.setComment(originalSe.getComment());
        String newNomEpreuve = "";
        int  x = 0 ;
        Long count = new Long(0);
        do {
          x++;
          count = sessionEpreuveRepository.countExistingNomSessionEpreuve(originalSe.getNomSessionEpreuve() + "(" + x + ")");
        } while (count!=0);
        newNomEpreuve = originalSe.getNomSessionEpreuve() +  "(" + x + ")";
        newSe.setNomSessionEpreuve(newNomEpreuve);
        newSe.setType(originalSe.getType());
        newSe.setHeureEpreuve(originalSe.getHeureEpreuve());
        newSe.setHeureConvocation(originalSe.getHeureConvocation());
        
        if(originalSe.getPlanSessionEpreuve()!=null) {
        	BigFile bigFile = new BigFile();
	        bigFile.setBinaryFile(originalSe.getPlanSessionEpreuve().getBigFile().getBinaryFile());
	        bigFile.setContext(context);
	        bigFileRepository.save(bigFile);
	        StoredFile storedFile = new StoredFile();
	        storedFile.setBigFile(bigFile);
	        StoredFile plan = originalSe.getPlanSessionEpreuve();
	        storedFile.setContentType(plan.getContentType());
	        storedFile.setContext(context);
	        storedFile.setFilename(plan.getFilename());
	        storedFile.setFileSize(plan.getFileSize());
	        storedFile.setImageData(plan.getImageData());
	        storedFile.setSendTime(new Date());
	        storedFileRepository.save(storedFile);
	        newSe.setPlanSessionEpreuve(storedFile);
        }
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
        
        List<TagCheck> listTc  = tagCheckRepository.findTagCheckBySessionEpreuveId(originalSe.getId());
        if(!listTc.isEmpty()) {
        	for(TagCheck t : listTc) {
        		TagCheck newTagCheck = new TagCheck();
        		newTagCheck.setContext(context);
        		newTagCheck.setGroupe(t.getGroupe());
        		newTagCheck.setIsTiersTemps(t.getIsTiersTemps());
        		newTagCheck.setIsUnknown(t.getIsUnknown());
        		newTagCheck.setNumAnonymat(t.getNumAnonymat());
        		newTagCheck.setPerson(t.getPerson());
        		newTagCheck.setSessionEpreuve(newSe);
        		tagCheckRepository.save(newTagCheck);
        	}
        }
       	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	log.info("Cpoie de la session : " + originalSe.getNomSessionEpreuve());
    	logService.log(ACTION.COPY_SESSION_EPREUVE, RETCODE.SUCCESS, originalSe.getNomSessionEpreuve() + " :: " + newSe.getNomSessionEpreuve(), ldapService.getEppn(auth.getName()), null, context.getKey(), null);

        return newSe;
	 }
}
