package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.repositories.custom.PersonRepositoryCustom;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.springframework.beans.factory.annotation.Autowired;
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
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
	
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
    
	public void getPdfPresence(HttpServletResponse response,  Long sessionLocationId, Long sessionEpreuveId) {
    	PdfPTable table = new PdfPTable(6);
    	
    	table.setWidthPercentage(100);
    	table.setHorizontalAlignment(Element.ALIGN_CENTER);
    	
    	List<TagCheck> list = tagCheckRepository.findTagCheckBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedIdOrderByPersonEppn(sessionLocationId, sessionLocationId);
    	tagCheckService.setNomPrenomTagChecks(list);
    	
    	Long totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
    	Long totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
    	Long totalNotExpected = tagCheckRepository.countTagCheckBySessionLocationExpectedIdIsNullAndSessionLocationBadgedId(sessionLocationId);
        //On créer l'objet cellule.
        String libelleSe = list.get(0).getSessionLocationExpected().getSessionEpreuve().getNomSessionEpreuve().concat(" ").
        		concat(String.format("%1$td-%1$tm-%1$tY", (list.get(0).getSessionLocationExpected().getSessionEpreuve().getDateExamen())).
        		concat(" à ").concat(list.get(0).getSessionLocationExpected().getLocation().getCampus().getSite()).concat(" --  ").
        		concat(list.get(0).getSessionLocationExpected().getLocation().getNom()).concat(" --  nb de présents :  ").
        		concat(totalPresent.toString()).concat("/").concat(totalExpected.toString())).concat((list.get(0).getSessionLocationExpected().getIsTiersTempsOnly())? " -- Temps aménagé" : "").
        		concat((totalNotExpected>0)? " -- nb d'intrus : " + totalNotExpected.toString() : "");
        		
        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
        cell.setBackgroundColor(BaseColor.GREEN);
        cell.setColspan(6);
        table.addCell(cell);
   
        //contenu du tableau.
        PdfPCell header1 = new PdfPCell(new Phrase("N°Identifiant")); header1.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header2 = new PdfPCell(new Phrase("Nom")); header2.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header3 = new PdfPCell(new Phrase("Prénom")); header3.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header4 = new PdfPCell(new Phrase("Présent")); header4.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header5 = new PdfPCell(new Phrase("Badgeage")); header5.setBackgroundColor(BaseColor.GRAY);
        PdfPCell header6 = new PdfPCell(new Phrase("Lieu badgé")); header6.setBackgroundColor(BaseColor.GRAY);
        
        table.addCell(header1);
        table.addCell(header2);
        table.addCell(header3);
        table.addCell(header4);
        table.addCell(header5);
        table.addCell(header6);
        
        
        if(!list.isEmpty()) {
        	for(TagCheck tc : list) {
        		String presence = "Absent";
        		String date  = "--";
        		PdfPCell dateCell = null;
        		String badged  = "--";
        		BaseColor b = new BaseColor(232, 97, 97, 50);
        		if(tc.getTagDate() != null) {
        			presence = "Présent";
        			date = String.format("%1$tH:%1$tM:%1$tS", tc.getTagDate());
        			b = new BaseColor(19, 232, 148, 50);
        		}
        		if(tc.getSessionLocationBadged()!=null && tc.getSessionLocationExpected() == null) {
        			b = new BaseColor(55, 0, 237, 50);
        		}
        		if(tc.getSessionLocationBadged()!=null) {
        			badged = tc.getSessionLocationBadged().getLocation().getNom();
        		}
        		dateCell = new PdfPCell(new Paragraph(tc.getPerson().getNumIdentifiant()));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(tc.getPerson().getNom()));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(tc.getPerson().getPrenom()));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(presence));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(date));
        		dateCell.setBackgroundColor(b);
                table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(badged));
        		dateCell.setBackgroundColor(b);
                table.addCell(dateCell);
        	}
        }
        
        Document document = new Document();
        document.setMargins(10, 10, 10, 10);
        try 
        {
          response.setContentType("application/pdf");
          PdfWriter.getInstance(document,  response.getOutputStream());
          document.open();

          document.add(table);

        } catch (DocumentException de) {
          de.printStackTrace();
        } catch (IOException de) {
          de.printStackTrace();
        }
       
        document.close();
    }
	
	public String getHandleRedirectUrl(EsupNfcTagLog esupNfcTagLog, String keyContext) throws ParseException {
		
		String url = "";
		
		Person person = personRepositoryCustom.findByEppn(esupNfcTagLog.getEppn()).get(0);
		
		if (person != null) {
			String locationNom = esupNfcTagLog.getLocation();
			String[] splitLocationNom = locationNom.split(" // ");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = dateFormat.parse(dateFormat.format(new Date()));
			TagCheck presentTagCheck = null;
			Long realSlId = null;
			Long sessionLocationId = tagCheckRepository.getSessionLocationId(splitLocationNom[1], esupNfcTagLog.getEppn(), date);
			if(sessionLocationId!=null) {
				realSlId = sessionLocationId;
				presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationId, esupNfcTagLog.getEppn());
			}else {
				//on regarde si la personne est dans une autre salle de la session
				Long slId = tagCheckRepository.getSessionLocationIdExpected(esupNfcTagLog.getEppn(), date);
				SessionLocation sl = sessionLocationRepository.findById(slId).get();
				realSlId = sl.getId();
				presentTagCheck = tagCheckRepository.findTagCheckBySessionEpreuveIdAndEppn(sl.getSessionEpreuve().getId(), esupNfcTagLog.getEppn());
			}
			Long sessionEpreuveId = presentTagCheck.getSessionEpreuve().getId();
			
			if(realSlId != null && sessionEpreuveId != null) {
				url =  keyContext + "/supervisor/presence?sessionEpreuve=" + sessionEpreuveId +  "&location=" + realSlId + "&tc=" + presentTagCheck.getId();
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
    	TagCheck presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn).get(0);
    	
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<UserLdap> userLdaps = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		TagChecker tagChecker =  (isPresent)? tagCheckerRepository.findTagCheckerByUserAppEppnEquals(userLdaps.get(0).getEppn(), null).getContent().get(0) : null;
		
    	presentTagCheck.setTagChecker(tagChecker);
    	presentTagCheck.setTagDate(date);
    	presentTagCheck.setSessionLocationBadged(sessionLocationBadged);
    	presentTagCheck.setContext(contextService.getcurrentContext());
    	presentTagCheck.setIsCheckedByCard((!isPresent)? null : false);
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
    	sessionLocationBadged.setNbPresentsSessionLocation(countPresent);
    	dataEmitterService.sendData(presentTagCheck, percent, totalPresent, 0, sessionLocationBadged);
    	return list;
	}
}
