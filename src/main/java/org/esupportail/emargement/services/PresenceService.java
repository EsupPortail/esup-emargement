package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.repositories.custom.PersonRepositoryCustom;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PresenceService {
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired	
	ContextRepository contextRepository;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
		
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
    @Resource
    UserLdapRepository userLdapRepository;
	
	@Autowired
	PersonRepositoryCustom personRepositoryCustom;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
    @Resource
    ContextService contextService;
    
    @Resource
	DataEmitterService dataEmitterService;
	
	@Resource
	LogService logService;
	
	@Autowired
    private MessageSource messageSource;
	
	public void getPdfPresence(HttpServletResponse response,  Long sessionLocationId, Long sessionEpreuveId, String emargementContext) {
		Long countProxyPerson = tagCheckRepository.countTagCheckBySessionEpreuveIdAndProxyPersonIsNotNull(sessionEpreuveId);
		int nbColumn = (countProxyPerson>0)? 9 : 8;
    	PdfPTable table = new PdfPTable(nbColumn);
    	
    	table.setWidthPercentage(100);
    	table.setHorizontalAlignment(Element.ALIGN_CENTER);
    	
    	List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIdOrderByPersonEppn(sessionEpreuveId, sessionLocationId);
    	tagCheckService.setNomPrenomTagChecks(list);
    	SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
    	SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
    	String nomFichier = "Export_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()));
    	nomFichier = nomFichier.replace(" ", "_");
    	Long totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
    	Long totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
        //On créer l'objet cellule.
        String libelleSe = se.getNomSessionEpreuve().concat(" ").
        		concat(String.format("%1$td-%1$tm-%1$tY", (se.getDateExamen())).
        		concat(" à ").concat(sl.getLocation().getCampus().getSite()).concat(" --  ").
        		concat(sl.getLocation().getNom()).concat(" --  nb de présents :  ").
        		concat(totalPresent.toString()).concat("/").concat(totalExpected.toString())).concat((sl.getIsTiersTempsOnly())? " -- Temps aménagé" : "");
        		
        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
        cell.setBackgroundColor(BaseColor.GREEN);
        cell.setColspan(nbColumn);
        table.addCell(cell);
   
        //contenu du tableau.
        PdfPCell header1 = new PdfPCell(new Phrase("N° Identifiant")); header1.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header2 = new PdfPCell(new Phrase("Eppn")); header2.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header3 = new PdfPCell(new Phrase("Nom")); header3.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header4 = new PdfPCell(new Phrase("Prénom")); header4.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header5 = new PdfPCell(new Phrase("Présent")); header5.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header6 = new PdfPCell(new Phrase("Emargement")); header6.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header7 = new PdfPCell(new Phrase("Type")); header7.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header8 = new PdfPCell(new Phrase("Lieu")); header8.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header9 = new PdfPCell(new Phrase("Procuration")); header9.setBackgroundColor(BaseColor.GRAY);
        
        table.addCell(header1);
        table.addCell(header2);
        table.addCell(header3);
        table.addCell(header4);
        table.addCell(header5);
        table.addCell(header6);
        table.addCell(header7);
        table.addCell(header8);        
        if(countProxyPerson>0) {
        	table.addCell(header9);
        }
        
        if(!list.isEmpty()) {
        	for(TagCheck tc : list) {
        		if(tc.getSessionLocationExpected()!=null) {
	        		String presence = "Absent";
	        		String date  = "--";
	        		PdfPCell dateCell = null;
	        		String badged  = "--";
	        		BaseColor b = new BaseColor(232, 97, 97, 50);
	        		String nom = "";
	        		String prenom = "";
	        		String identifiant = "";
	        		String numIdentifiant = "";
	        		String typeemargement = "";
	        		if(tc.getPerson() !=null ) {
	        			nom = tc.getPerson().getNom();
	        			prenom = tc.getPerson().getPrenom();
	        			identifiant = tc.getPerson().getEppn();
	        			numIdentifiant = tc.getPerson().getNumIdentifiant();
	        		}else if(tc.getGuest() !=null ) {
	        			nom = tc.getGuest().getNom();
	        			prenom = tc.getGuest().getPrenom();
	        			identifiant = tc.getGuest().getEmail();
	        		}
	        		if(tc.getTagDate() != null) {
	        			presence = "Présent";
	        			date = String.format("%1$tH:%1$tM:%1$tS", tc.getTagDate());
	        			b = new BaseColor(19, 232, 148, 50);
	        		}
	        		String type = "";
	        		if(tc.getTypeEmargement()!=null) {
	        			type = tc.getTypeEmargement().name();
	        			typeemargement = messageSource.getMessage("typeEmargement.".concat(type.toLowerCase()), null, null);
	        		}
	        		if(tc.getSessionLocationBadged()!=null && tc.getSessionLocationExpected() == null) {
	        			b = new BaseColor(55, 0, 237, 50);
	        		}
	        		if(tc.getSessionLocationBadged()!=null) {
	        			badged = tc.getSessionLocationBadged().getLocation().getNom();
	        		}
	        		dateCell = new PdfPCell(new Paragraph(numIdentifiant));
	        		dateCell.setBackgroundColor(b);
	        		table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(identifiant));
	        		dateCell.setBackgroundColor(b);
	        		table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(nom));
	        		dateCell.setBackgroundColor(b);
	        		table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(prenom));
	        		dateCell.setBackgroundColor(b);
	        		table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(presence));
	        		dateCell.setBackgroundColor(b);
	        		table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(date));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(typeemargement));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	        		dateCell = new PdfPCell(new Paragraph(badged));
	        		dateCell.setBackgroundColor(b);
	                table.addCell(dateCell);
	                if(countProxyPerson>0) {
		        		dateCell = new PdfPCell(new Paragraph((tc.getProxyPerson()!=null)? tc.getProxyPerson().getPrenom() + ' ' + tc.getProxyPerson().getNom(): ""));
		        		dateCell.setBackgroundColor(b);
		                table.addCell(dateCell);
	                }
        		}
        	}
        }
        
        Document document = new Document();
        document.setMargins(10, 10, 10, 10);
        try 
        {
          response.setContentType("application/pdf");
          response.setHeader("Content-Disposition","attachment; filename=".concat(nomFichier));
          PdfWriter.getInstance(document,  response.getOutputStream());
          document.open();

          document.add(table);
          logService.log(ACTION.EXPORT_PDF, RETCODE.SUCCESS, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        } catch (DocumentException de) {
          de.printStackTrace();
          logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        } catch (IOException de) {
          de.printStackTrace();
          logService.log(ACTION.EXPORT_PDF, RETCODE.FAILED, "Extraction pdf :" +  list.size() + " résultats" , null,
					null, emargementContext, null);
        }
       
        document.close();
    }
	
	public String getHandleRedirectUrl(EsupNfcTagLog esupNfcTagLog, String keyContext) throws ParseException {
		
		String url = "";
		
		Person person = personRepositoryCustom.findByEppn(esupNfcTagLog.getEppn()).get(0);
		
		if (person != null) {
			String locationNom = esupNfcTagLog.getLocation();
			String[] splitLocationNom = locationNom.split(" // ");
			String nomSessionEpreuve = splitLocationNom[0];
			String nomLocation = splitLocationNom[1];
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = dateFormat.parse(dateFormat.format(new Date()));
			TagCheck presentTagCheck = null;
			Long realSlId = null;
			Long sessionLocationId = tagCheckRepository.getSessionLocationId(nomLocation, esupNfcTagLog.getEppn(), date, nomSessionEpreuve);
			SessionEpreuve se = sessionEpreuveRepository.findByNomSessionEpreuve(nomSessionEpreuve, null).getContent().get(0);
			Long sessionEpreuveId = se.getId();
			String urlPresent = "";
			if(sessionLocationId!=null) {
				realSlId = sessionLocationId;
				presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationId, esupNfcTagLog.getEppn());
				urlPresent = "&tc=" + presentTagCheck.getId();
			}else {
				Long slId = tagCheckRepository.getSessionLocationIdExpected(esupNfcTagLog.getEppn(), date, nomSessionEpreuve);
				SessionLocation sl = sessionLocationRepository.findById(slId).get();
				Long count = tagCheckerRepository.countBySessionLocationIdAndUserAppEppn(sl.getId(), esupNfcTagLog.getEppnInit());
				//on regrde si le surveillant à accès à la salle pour la vue...
				if(count == 0) {
					List<TagChecker> tcs = tagCheckerRepository.findTagCheckerBySessionLocationLocationNomAndSessionLocationSessionEpreuveNomSessionEpreuveAndUserAppEppn(nomLocation, nomSessionEpreuve, esupNfcTagLog.getEppnInit());
					if(!tcs.isEmpty()) {
						realSlId = tcs.get(0).getSessionLocation().getId();
					}
				}else {
					//on regarde si la personne est dans une autre salle de la session
					realSlId = sl.getId();
					presentTagCheck = tagCheckRepository.findTagCheckBySessionEpreuveIdAndEppn(sl.getSessionEpreuve().getId(), esupNfcTagLog.getEppn());
					urlPresent = "&tc=" + presentTagCheck.getId();
				}
			}
			if(realSlId != null && sessionEpreuveId != null) {
				url =  keyContext + "/supervisor/presence?sessionEpreuve=" + sessionEpreuveId +  "&location=" + realSlId + urlPresent;
			}
		}
		
		return url;
	}
	
	public List<TagCheck> updatePresents(String presence) {
		
		String [] splitPresence = presence.split(",");
		boolean isPresent = Boolean.valueOf(splitPresence[0].trim());
    	String eppn = splitPresence[1].trim();
    	Long sessionLocationId = Long.valueOf(splitPresence[2].trim());
    	Date date = (isPresent)? new Date() : null;
    	SessionLocation sessionLocationBadged = (isPresent)? sessionLocationRepository.findById(sessionLocationId).get() : null;
    	String email = (splitPresence.length >3)? splitPresence[3] : "";
    	TagCheck presentTagCheck = null;
    	if(!eppn.isEmpty()) {
    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn).get(0);
    	}else if(!email.isEmpty()) {
    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndGuestEmailEquals(sessionLocationId, email).get(0);
    	}
    	TypeEmargement typeEmargement = null;
    	if(isPresent) {
    		if(splitPresence.length >4) {
	    		if("qrcode".equals(splitPresence[4].trim())){
	    			typeEmargement = TypeEmargement.QRCODE;
	    		}
	    	}else {
	    		typeEmargement = TypeEmargement.MANUAL;
	    	}
    	}
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<UserLdap> userLdaps = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		TagChecker tagChecker =  (isPresent)? tagCheckerRepository.findTagCheckerByUserAppEppnEquals(userLdaps.get(0).getEppn(), null).getContent().get(0) : null;
		
    	presentTagCheck.setTagChecker(tagChecker);
    	presentTagCheck.setTagDate(date);
    	presentTagCheck.setSessionLocationBadged(sessionLocationBadged);
    	presentTagCheck.setContext(contextService.getcurrentContext());
    	presentTagCheck.setTypeEmargement(typeEmargement);
    	tagCheckRepository.save(presentTagCheck);
    	List<TagCheck> list = new ArrayList<TagCheck>();
    	list.add(presentTagCheck);
    	tagCheckService.setNomPrenomTagChecks(list);
    	if(presentTagCheck.getTagChecker() != null) {
        	List<TagChecker> tcList = new ArrayList<TagChecker>();
        	tcList.add(presentTagCheck.getTagChecker());
	    	tagCheckerService.setNomPrenom4TagCheckers(tcList);
	    	presentTagCheck.setTagChecker(tcList.get(0));
    	}
    	
    	Long totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
    	Long totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
    	float percent = 0;
    	if(totalExpected!=0) {
    		percent = 100*(new Long(totalPresent).floatValue()/ new Long(totalExpected).floatValue() );
    	}
    	Long countPresent = tagCheckRepository.countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(sessionLocationBadged);
    	if(sessionLocationBadged != null) {
    		sessionLocationBadged.setNbPresentsSessionLocation(countPresent);
    		dataEmitterService.sendData(presentTagCheck, percent, totalPresent, 0, sessionLocationBadged);
    	}
    	
    	return list;
	}
	
	public void updatePrefs(String nom, String value, String eppn, String key) {
		List<Prefs> prefs = prefsRepository.findByUserAppEppnAndNom(eppn, nom);
		Prefs pref = null;
		if(!prefs.isEmpty()) {
			pref = prefs.get(0);
			pref.setValue(value);
		}else {
			Context context = contextRepository.findByContextKey(key);
			pref = new Prefs();
			UserApp userApp = userAppRepository.findByEppnAndContext(eppn, context);
			pref.setUserApp(userApp);
			pref.setContext(context);
			pref.setNom(nom);
			pref.setValue(value);
		}
		prefsRepository.save(pref);
	}
	
	public void saveTagCheckSessionLibre(Long slId, String eppn, String emargementContext, SessionLocation sl) {
		
    	List<TagCheck> existingTc = tagCheckRepository.findTagCheckBySessionLocationBadgedIdAndPersonEppnEquals(slId, eppn);
    	
    	if(existingTc.isEmpty()) {
    		Long nbTc =  tagCheckRepository.countBySessionLocationExpectedId(slId);
    		if(nbTc < sl.getCapacite()){
		    	List<Person> list = personRepository.findByEppn(eppn);
		    	Context context = contextRepository.findByContextKey(emargementContext);
		    	Person p = null;
		    	UserLdap user = userLdapRepository.findByEppnEquals(eppn).get(0);
		    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		    	UserLdap authUser = userLdapRepository.findByUid(auth.getName()).get(0);
		    	TagChecker tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(authUser.getEppn(), null).getContent().get(0);
		    	if(!list.isEmpty()) {
		    		p = list.get(0);
		    	}else {
		    		p = new Person();
		    		p.setEppn(eppn);
		    		p.setContext(context);
		    		String type = (user.getNumEtudiant() == null)? "staff" : "student";
		    		p.setType(type);
		    		p.setNumIdentifiant(user.getNumEtudiant());
		    		personRepository.save(p);
		    	}
		    	TagCheck tc = new TagCheck();
		    	tc.setContext(context);
		    	tc.setTypeEmargement(TypeEmargement.MANUAL);
		    	tc.setPerson(p);
		    	tc.setSessionEpreuve(sl.getSessionEpreuve());
		    	tc.setSessionLocationBadged(sl);
		    	tc.setSessionLocationExpected(sl);
		    	tc.setTagChecker(tagChecker);
		    	tc.setTagDate(new Date());
		    	tc.setIsTiersTemps(sl.getIsTiersTempsOnly());
		    	tagCheckRepository.save(tc);
    		}
    	}
	}
}
