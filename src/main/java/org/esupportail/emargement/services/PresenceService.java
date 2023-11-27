package org.esupportail.emargement.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.custom.PersonRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService.AppliConfigKey;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.utils.ToolUtil;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
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
	UserAppRepository userAppRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
		
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
    @Resource
    LdapUserRepository ldapUserRepository;
    
    @Resource    
    AppliConfigRepository appliConfigRepository;
	
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
    AppliConfigService appliConfigService;
	
	@Resource
	LogService logService;
	
	@Resource
	GroupeService groupeService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired	
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;
	
	@Autowired
    private MessageSource messageSource;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void getPdfPresence(Document document, HttpServletResponse response,  Long sessionLocationId, String emargementContext, ByteArrayOutputStream bos) {
        document.setMargins(10, 10, 10, 10);
       
        SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
        SessionEpreuve se = sl.getSessionEpreuve();
        List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(se.getId(), null).getContent();
    	tagCheckService.setNomPrenomTagChecks(list, false, false);
        String dateFin = (se.getDateFin()!=null)? "" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin())) : "";
        String date = String.format("%1$td-%1$tm-%1$tY", (se.getDateExamen())).concat("\n"  + dateFin);
        String nomFichier = "Export_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").
    			concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()).concat(dateFin));
        String heures = String.format("%1$tH:%1$tM", se.getHeureEpreuve()) + " - " + String.format("%1$tH:%1$tM", se.getFinEpreuve());
        Long totalExpected = tagCheckRepository.countBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(se.getId());
    	Long totalPresent = tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNull(se.getId());
    	String total = String.valueOf(totalPresent) + " / " + String.valueOf(totalExpected);
    	String title = se.getNomSessionEpreuve() + " // " + sl.getLocation().getNom();
    	List<TagChecker> tagCheckers = tagCheckerRepository.findTagCheckerBySessionLocationSessionEpreuveId(se.getId());
    	tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
    	String surveillants = tagCheckers.stream()
                .map(t-> (t.getUserApp().getPrenom()+ "-" + t.getUserApp().getNom())).distinct()
                .collect(Collectors.joining(","));
        try 
        {
			
			PdfWriter writer =   null;
			if(bos != null) {
				writer = PdfWriter.getInstance(document, bos);
			}else {
				response.setContentType("application/pdf");
				response.setHeader("Content-Disposition", "attachment; filename=".concat(nomFichier));
				writer = PdfWriter.getInstance(document, response.getOutputStream());
			}
			
			Image image = Image.getInstance(PresenceService.class.getResource("/static/images/logo.jpg"));
			image.scaleAbsolute(150f, 50f);//image width,height 

			PdfPTable headerTable = new PdfPTable(3);
			headerTable.addCell(pdfGenaratorUtil.getIRDCell("Présents"));
			headerTable.addCell(pdfGenaratorUtil.getIRDCell("Date"));
			headerTable.addCell(pdfGenaratorUtil.getIRDCell("Heure"));
			headerTable.addCell(pdfGenaratorUtil.getIRDCell(total)); 
			headerTable.addCell(pdfGenaratorUtil.getIRDCell(date)); 	
			headerTable.addCell(pdfGenaratorUtil.getIRDCell(heures));	
			
			Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.BOLD);
			Paragraph name = new Paragraph(title, font);
			name.setSpacingBefore(2f);

			PdfPTable tagCheckerTable = new PdfPTable(1);
			tagCheckerTable.setWidthPercentage(100);
			tagCheckerTable.addCell(pdfGenaratorUtil.getTagCheckerCell("Surveillants :"));
			tagCheckerTable.addCell(pdfGenaratorUtil.getTagCheckerCell(surveillants));
			PdfPCell summaryL = new PdfPCell (tagCheckerTable);
			summaryL.setColspan (4);
			summaryL.setPadding (1.0f);	                   
			
			PdfPTable remarques = new PdfPTable(1);
			remarques.setWidthPercentage(100);
			remarques.addCell(pdfGenaratorUtil.getRemarquesCell("Remarques: " + se.getComment()));
			PdfPCell summaryR = new PdfPCell (remarques);
			summaryR.setColspan (3);         

			PdfPTable describer = new PdfPTable(1);
			describer.setWidthPercentage(100);
			describer.addCell(pdfGenaratorUtil.getDescCell(" "));
			describer.addCell(pdfGenaratorUtil.getDescCell("Esup-emargement - " + Year.now().getValue()));	

			document.open();
			
			headerTable.setTotalWidth(300f);
			headerTable.writeSelectedRows(0, -1, document.right() - headerTable.getTotalWidth(), document.top(),writer.getDirectContent());
			
			PdfPTable mainTable = new PdfPTable(7); 
			mainTable.setWidthPercentage(100);
			mainTable.setWidths(new float[] { 0.7f,1.5f,1.5f,2,0.8f,1.5f,1.4f});
			mainTable.setSpacingBefore(20.0f);
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("#"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Nom"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Prénom"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Identifiant"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Type"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Emargement"));
			mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Mode"));
			document.add(image);
			document.add(name);
			
	        if(!list.isEmpty()) {
	        	int i =0;int j =1;
	        	for(TagCheck tc : list) {
	        		String dateEmargement  = "";
	        		String nom = "";
	        		String prenom = "";
	        		String identifiant = "";
	        		String typeemargement = "";
	        		String typeIndividu = "";
	        		if(tc.getPerson() !=null ) {
	        			nom = tc.getPerson().getNom();
	        			prenom = tc.getPerson().getPrenom();
	        			identifiant = tc.getPerson().getNumIdentifiant();
	        			if(identifiant == null) {
	        				identifiant = tc.getPerson().getEppn();
	        			}
	        			typeIndividu = messageSource.getMessage("person.type.".concat(tc.getPerson().getType()), null, null).substring(0,1);
	        		}else if(tc.getGuest() !=null ) {
	        			nom = tc.getGuest().getNom();
	        			prenom = tc.getGuest().getPrenom();
	        			identifiant = tc.getGuest().getEmail();
	        			typeIndividu = "Ex";
	        		}
	        		if(BooleanUtils.isTrue(tc.getIsExempt())) {
        				dateEmargement = "Exempt";
	        		}else if(tc.getTagDate() != null) {
	        			dateEmargement = String.format("%1$tH:%1$tM", tc.getTagDate());
	        			if(tc.getIsUnknown()) {
	        				dateEmargement = "Inconnu";
	        			}
	        		}
	        		if(tc.getIsTiersTemps()) {
	        			dateEmargement += " \nTemps aménagé";
	        		}
	        		if(tc.getTypeEmargement()!=null) {
	        			typeemargement = messageSource.getMessage("typeEmargement.".concat(tc.getTypeEmargement().name().toLowerCase()), null, null) + "\n";
	        		}
	        		typeemargement += (tc.getProxyPerson()!=null)? "Proc : " + tc.getProxyPerson().getPrenom() + ' ' + tc.getProxyPerson().getNom(): "";
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(String.valueOf(j)));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(nom));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(prenom));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(identifiant));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(typeIndividu));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(dateEmargement));
					mainTable.addCell(pdfGenaratorUtil.getMainRowCell(typeemargement));
					i++;
					if (i == 18) {
						mainTable.addCell(summaryL);
						mainTable.addCell(summaryR);
						document.add(mainTable);
						document.add(describer);
						document.newPage();
						document.add(image);
						headerTable.setTotalWidth(300f);
						headerTable.writeSelectedRows(0, -1, document.right() - headerTable.getTotalWidth(), document.top(),
								writer.getDirectContent());
						document.add(name);
						mainTable = new PdfPTable(7);
						mainTable.setWidthPercentage(100);
						mainTable.setWidths(new float[] { 0.7f,1.5f,1.5f,2,0.8f,1.5f,1.4f});
						mainTable.setSpacingBefore(20.0f);
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("#"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Nom"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Prénom"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Identifiant"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Type"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Emargement"));
						mainTable.addCell(pdfGenaratorUtil.getMainHeaderCell("Mode"));
						i=0;
					}
					j++;
	        	}
	        }
			mainTable.addCell(summaryL);
			mainTable.addCell(summaryR);
			document.add(mainTable);
			document.add(describer);
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
    }

	public String getHandleRedirectUrl(EsupNfcTagLog esupNfcTagLog, String keyContext) throws ParseException {
		
		String url = "";
		
		Person person = personRepositoryCustom.findByEppn(esupNfcTagLog.getEppn()).get(0);
		
		if (person != null) {
			String locationNom = esupNfcTagLog.getLocation();
			String[] splitLocationNom = locationNom.split(" // "); 
			String nomSalle = splitLocationNom[2];
			String idSession = splitLocationNom[3];
			Long id = Long.valueOf(idSession);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = dateFormat.parse(dateFormat.format(new Date()));
			TagCheck presentTagCheck = null;
			Long realSlId = null;
			Long sessionLocationId =  null;
			SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
			Date dateFin = (sessionEpreuve.getDateFin()!=null)?  dateFormat.parse(dateFormat.format(sessionEpreuve.getDateFin())) : null;
			if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
				sessionLocationId = tagCheckRepository.getSessionLocationId(nomSalle, esupNfcTagLog.getEppn(), date, id);
			}else {
				sessionLocationId = tagCheckRepository.getSessionLocationIdWithDateFin(nomSalle, esupNfcTagLog.getEppn(), date, dateFin, id);
			}
			String urlPresent = "";
			if(sessionLocationId!=null) {
				realSlId = sessionLocationId;
				presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndEppn(sessionLocationId, esupNfcTagLog.getEppn());
				urlPresent = "&tc=" + presentTagCheck.getId();
			}else {
				Long slId = tagCheckRepository.getSessionLocationIdExpected(esupNfcTagLog.getEppn(), date, id);
				SessionLocation sl = sessionLocationRepository.findById(slId).get();
				Long count = tagCheckerRepository.countBySessionLocationIdAndUserAppEppn(sl.getId(), esupNfcTagLog.getEppnInit());
				//on regrde si le surveillant à accès à la salle pour la vue...
				if(count == 0) {
					List<TagChecker> tcs = tagCheckerRepository.findTagCheckerBySessionLocationLocationNomAndSessionLocationSessionEpreuveIdAndUserAppEppn(nomSalle, id, esupNfcTagLog.getEppnInit());
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
			Groupe gpe = sessionEpreuve.getBlackListGroupe();
			boolean isBlackListed = groupeService.isBlackListed(gpe, esupNfcTagLog.getEppn());
			if(isBlackListed){
				urlPresent = "&msgError=" + esupNfcTagLog.getEppn();
			}
			if(BooleanUtils.isTrue(sessionEpreuve.getIsSaveInExcluded())) {
				List <Long> idsGpe = new ArrayList<Long>();
				idsGpe.add(gpe.getId());
				groupeService.addMember(esupNfcTagLog.getEppn(),idsGpe);
			}
			if(realSlId != null && id != null) {
				url =  keyContext + "/supervisor/presence?sessionEpreuve=" + id +  "&location=" + realSlId + urlPresent;
			}
		}
		
		return url;
	}
	
	public List<TagCheck> updatePresents(String presence) {
		String eppn = null;
		boolean isTagCheckerNeeded = true;
		boolean isValid = true;
		List<TagCheck> list = new ArrayList<TagCheck>();
		if(presence.startsWith("qrcode")) {
			Long qrCodeValidtime = Long.valueOf(appliConfigService.getQrCodeChange());
			Long now = System.currentTimeMillis() / 1000;
			if(presence.contains("@@@")) {
				isTagCheckerNeeded = false;
				String splitPresence [] = presence.split("@@@");
				presence = splitPresence[0];
				eppn = toolUtil.decodeFromBase64(splitPresence[1]);
			}
			String temp = toolUtil.decodeFromBase64(presence.replace("qrcode", ""));
			String [] splitTemp = temp.split("@@@");
			String qrCodetimestamp = splitTemp[1];
			presence = splitTemp[0];
			if(now - Long.valueOf(qrCodetimestamp) > qrCodeValidtime) {
				isValid = false;
				Long tempsDepasse = now - Long.valueOf(qrCodetimestamp) + qrCodeValidtime;
				log.info("QrCode invalide pour " + eppn + ", temps dépassé de " + tempsDepasse + " secondes");
			}
		}
		if(isValid) {
			String [] splitPresence = presence.split(",");
			if(splitPresence.length>0) {
				String msgError = "";
				TypeEmargement typeEmargement = null;
				boolean isPresent = Boolean.valueOf(splitPresence[0].trim());
		    	if(isPresent) {
		    		if(splitPresence.length >4) {
			    		if("qrcode".equals(splitPresence[4].trim())){
			    			typeEmargement = TypeEmargement.QRCODE;
			    		}
			    	}else {
			    		typeEmargement = TypeEmargement.MANUAL;
			    	}
		    	}
		    	if(eppn == null) {
		    		eppn = splitPresence[1].trim();
		    	}
		    	Long sessionLocationId = Long.valueOf(splitPresence[2].trim());
		    	Date date = (isPresent)? new Date() : null;
		    	SessionLocation sessionLocationBadged = (isPresent)? sessionLocationRepository.findById(sessionLocationId).get() : null;
		    	
		    	if(sessionLocationRepository.findById(sessionLocationId).get() != null) {
			    	SessionEpreuve se = sessionLocationRepository.findById(sessionLocationId).get().getSessionEpreuve();
			    	Groupe gpe = se.getBlackListGroupe();
					boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);	
			    	String email = (splitPresence.length >3)? splitPresence[3] : "";
			    	TagCheck presentTagCheck = null;
			    	if(!eppn.isEmpty()) {
			    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(sessionLocationId, eppn).get(0);
			    	}else if(!email.isEmpty()) {
			    		presentTagCheck = tagCheckRepository.findTagCheckBySessionLocationExpectedIdAndGuestEmailEquals(sessionLocationId, email).get(0);
			    	}
			    	presentTagCheck.setSessionEpreuve(se);
			    	presentTagCheck.setTypeEmargement(typeEmargement);
			    	presentTagCheck.setNbBadgeage(tagCheckService.getNbBadgeage(presentTagCheck, isPresent));
			    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				    if(isTagCheckerNeeded) {
						TagChecker tagChecker =  (isPresent)? tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0) : null;
						presentTagCheck.setTagChecker(tagChecker);
			    	}
					if(!isPresent) {
						presentTagCheck.setProxyPerson(null);
					}
			    	presentTagCheck.setSessionLocationBadged(sessionLocationBadged);
			    	
			    	presentTagCheck.setTagDate(date);
			    	presentTagCheck.setContext(contextService.getcurrentContext());
			    	if(!isBlackListed && isPresent || !isBlackListed && !isPresent || isBlackListed && !isPresent) {
			    		tagCheckRepository.save(presentTagCheck);
			    	}
			    	
			    	list.add(presentTagCheck);
			    	tagCheckService.setNomPrenomTagChecks(list, false, false);
			    	if(presentTagCheck.getTagChecker() != null) {
			        	List<TagChecker> tcList = new ArrayList<TagChecker>();
			        	tcList.add(presentTagCheck.getTagChecker());
				    	tagCheckerService.setNomPrenom4TagCheckers(tcList);
				    	presentTagCheck.setTagChecker(tcList.get(0));
			    	}
					if(isBlackListed) {
						msgError = presentTagCheck.getNomPrenom();
					}
			    	Long totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
			    	Long totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
			    	float percent = 0;
			    	if(totalExpected!=0) {
			    		percent = 100*(new Long(totalPresent).floatValue()/ new Long(totalExpected).floatValue() );
			    	}
			    	//Long countPresent = tagCheckRepository.countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(sessionLocationBadged);
			        if(sessionLocationBadged != null) {
			    		sessionLocationBadged.setNbPresentsSessionLocation(totalPresent);
			    	}
			        dataEmitterService.sendData(presentTagCheck, percent, totalPresent, sessionLocationBadged, msgError);
	    		}
			}
		}
    	return list;
	}
	
	public boolean saveTagCheckSessionLibre(Long slId, String eppn, String emargementContext, SessionLocation sl) {
		
		Groupe gpe = sl.getSessionEpreuve().getBlackListGroupe();
		boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);
		
		if(!isBlackListed) {
			List<TagCheck> existingTc = tagCheckRepository.findTagCheckBySessionLocationBadgedIdAndPersonEppnEquals(slId, eppn);
    	
	    	if(existingTc.isEmpty()) {
	    		Long nbTc =  tagCheckRepository.countBySessionLocationExpectedId(slId);
	    		if(nbTc < sl.getCapacite()){
			    	List<Person> list = personRepository.findByEppn(eppn);
			    	Context context = contextRepository.findByContextKey(emargementContext);
			    	Person p = null;
			    	LdapUser user = ldapUserRepository.findByEppnEquals(eppn).get(0);
			    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			    	TagChecker tagChecker = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0);
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
			    	tc.setNbBadgeage(tagCheckService.getNbBadgeage(tc, true));
			    	tc.setIsTiersTemps(sl.getIsTiersTempsOnly());
			    	tagCheckRepository.save(tc);
	    		}
	    	}
    	}
		
		return isBlackListed;
	}
	
	public String getBase64Photo(EsupNfcTagLog taglog) {
		String photo64 = null;
		String locationNom = taglog.getLocation();
		String[] splitLocationNom = locationNom.split(" // ");
		String idSession = splitLocationNom[3];
		Long id = Long.valueOf(idSession);
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
		Context ctx = sessionEpreuve.getContext();
		List<AppliConfig> list = appliConfigRepository.findAppliConfigByKeyAndContext(AppliConfigKey.ENABLE_PHOTO_ESUPNFCTAG.name(), ctx);
		log.info(list.get(0).getValue());
		if(!list.isEmpty() && list.get(0).getValue().equals("true")) {
			String eppn = taglog.getEppn();
			RestTemplate template = new RestTemplate();
			String uri = null;
			byte[] photo = null;
			Boolean noPhoto = true;
			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> httpResponse = new ResponseEntity<byte[]>(photo, headers, HttpStatus.OK);
			if(!"inconnu".equals(eppn)) {
				headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
				MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
				HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
				uri = photoPrefixe.concat(eppn).concat(photoSuffixe);
					noPhoto = false;
					httpResponse = template.exchange(uri, HttpMethod.GET, request, byte[].class);
					if(httpResponse.getBody() == null) noPhoto = true;
			}
			if (noPhoto) {
				ClassPathResource noImg = new ClassPathResource("NoPhoto.png");
				try {
					photo = IOUtils.toByteArray(noImg.getInputStream());
					httpResponse = new ResponseEntity<byte[]>(photo, headers, HttpStatus.OK);
				} catch (IOException e) {
					log.info("IOException reading ", e);
				}
			}
			photo64 = Base64Utils.encodeToString(httpResponse.getBody());
		}
		return photo64;
	}

}
