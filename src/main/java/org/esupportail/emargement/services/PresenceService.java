package org.esupportail.emargement.services;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.custom.PersonRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService.AppliConfigKey;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
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
	LogService logService;
	
	@Resource
	GroupeService groupeService;
	
	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;
	
	@Autowired
    private MessageSource messageSource;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void getPdfPresence(HttpServletResponse response,  Long sessionLocationId, Long sessionEpreuveId, String emargementContext) {

        Document document = new Document();
        document.setMargins(10, 10, 10, 10);
        SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
        SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
        List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(sessionEpreuveId, null).getContent();
        String dateFin = (se.getDateFin()!=null)? "_" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin())) : "";
        String nomFichier = "Export_".concat(se.getNomSessionEpreuve()).concat("_").concat(sl.getLocation().getNom()).concat("_").
    			concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()).concat(dateFin));
        PdfPTable table = getTablePdf(response, sl, se, emargementContext);
        try 
        {
          response.setContentType("application/pdf");
          response.setHeader("Content-Disposition","attachment; filename=".concat(nomFichier));
          PdfWriter.getInstance(document,  response.getOutputStream());
          document.open();
          document.add(table);
          Paragraph paragraph = new Paragraph(se.getComment());
          paragraph.setSpacingBefore(10f);
          document.add(paragraph);
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
	
	public PdfPTable getTablePdf(HttpServletResponse response, SessionLocation sl, SessionEpreuve se, String emargementContext) {
		
		Long countProxyPerson = tagCheckRepository.countTagCheckBySessionEpreuveIdAndProxyPersonIsNotNull(se.getId());
		int nbColumn = (countProxyPerson>0)? 8 : 7;
		if(BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
			nbColumn = nbColumn + 1;
		}
    	PdfPTable table = new PdfPTable(nbColumn);
    	String nbInconnus = "";
    	Long inconnuTotal = new Long(0);
    	inconnuTotal = tagCheckRepository.countBySessionEpreuveIdAndIsUnknownTrue(se.getId());
		if(inconnuTotal>0) {
			nbInconnus = " --  nb d'inconnus :  " + inconnuTotal;
		}
    	
    	table.setWidthPercentage(100);
    	table.setHorizontalAlignment(Element.ALIGN_CENTER);
    	
    	List<TagCheck> list = tagCheckRepository.findTagCheckBySessionEpreuveIdOrderByPersonEppn(se.getId(), null).getContent();
    	tagCheckService.setNomPrenomTagChecks(list);
    	String dateFin = (se.getDateFin()!=null)? "_" + String.format("%1$td-%1$tm-%1$tY", (se.getDateFin())) : "";
    	String nomFichier = "Export_".concat(se.getNomSessionEpreuve()).concat("_").concat(String.format("%1$td-%1$tm-%1$tY", se.getDateExamen()).concat(dateFin));
    	nomFichier = nomFichier.replace(" ", "_");
    	Long totalExpected = tagCheckRepository.countBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(se.getId());
    	Long totalPresent = tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNull(se.getId());
    	
        //On créer l'objet cellule.
        String libelleSe = se.getNomSessionEpreuve().concat(" ").
        		concat(String.format("%1$td-%1$tm-%1$tY", (se.getDateExamen())).concat(dateFin).
        		concat(" à ").concat(sl.getLocation().getCampus().getSite()).
        		concat(" --  nb de présents :  ").
        		concat(totalPresent.toString()).concat("/").concat(totalExpected.toString())).concat(nbInconnus).concat((sl.getIsTiersTempsOnly())? " -- Temps aménagé" : "");
        		
        PdfPCell cell = new PdfPCell(new Phrase(libelleSe));
        cell.setBackgroundColor(BaseColor.GREEN);
        cell.setColspan(nbColumn);
        table.addCell(cell);
   
        //contenu du tableau.
        PdfPCell header1 = new PdfPCell(new Phrase("Identifiant")); header1.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header3 = new PdfPCell(new Phrase("Nom")); header3.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header4 = new PdfPCell(new Phrase("Prénom")); header4.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header41 = new PdfPCell(new Phrase("Groupe")); header41.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header5 = new PdfPCell(new Phrase("Type")); header5.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header6 = new PdfPCell(new Phrase("Emargement")); header6.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header7 = new PdfPCell(new Phrase("Mode")); header7.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header8 = new PdfPCell(new Phrase("Lieu")); header8.setBackgroundColor(BaseColor.LIGHT_GRAY);
        PdfPCell header9 = new PdfPCell(new Phrase("Procuration")); header9.setBackgroundColor(BaseColor.LIGHT_GRAY);
        
        table.addCell(header1);
        table.addCell(header3);
        table.addCell(header4);
        if(BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
        	table.addCell(header41);
        }        
        table.addCell(header5);
        table.addCell(header6);
        table.addCell(header7);
        table.addCell(header8);        
        if(countProxyPerson>0) {
        	table.addCell(header9);
        }
        
        if(!list.isEmpty()) {
        	for(TagCheck tc : list) {
        		String date  = "";
        		PdfPCell dateCell = null;
        		String badged  = "";
        		BaseColor b = new BaseColor(232, 97, 97, 50);
        		String nom = "";
        		String prenom = "";
        		String identifiant = "";
        		String typeemargement = "";
        		String typeIndividu = "";
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
        		if(tc.getTagDate() != null) {
        			date = String.format("%1$tH:%1$tM:%1$tS", tc.getTagDate());
        			b = new BaseColor(19, 232, 148, 50);
        			if(tc.getIsUnknown()) {
        				b = new BaseColor(237, 223, 14, 50);
        			}
        		}
        		if(BooleanUtils.isTrue(tc.getIsExempt())) {
        		}
        		String type = "";
        		if(tc.getTypeEmargement()!=null) {
        			type = tc.getTypeEmargement().name();
        			typeemargement = messageSource.getMessage("typeEmargement.".concat(type.toLowerCase()), null, null);
        		}
        		if(tc.getSessionLocationBadged()!=null) {
        			badged = tc.getSessionLocationBadged().getLocation().getNom();
        		}
        		dateCell = new PdfPCell(new Paragraph(identifiant));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(nom));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		dateCell = new PdfPCell(new Paragraph(prenom));
        		dateCell.setBackgroundColor(b);
        		table.addCell(dateCell);
        		if(BooleanUtils.isTrue(se.getIsGroupeDisplayed())) {
	        		dateCell = new PdfPCell(new Paragraph(groupe));
	        		dateCell.setBackgroundColor(b);
	        		table.addCell(dateCell);
        		}
         		dateCell = new PdfPCell(new Paragraph(typeIndividu));
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
		return table;
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
			Long sessionLocationId =  null;
			SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findByNomSessionEpreuve(splitLocationNom[0], null).getContent().get(0);
			Date dateFin = (sessionEpreuve.getDateFin()!=null)?  dateFormat.parse(dateFormat.format(sessionEpreuve.getDateFin())) : null;
			if(dateFin == null || dateFin.equals(sessionEpreuve.getDateExamen()) && dateFin.equals(date)){
				sessionLocationId = tagCheckRepository.getSessionLocationId(nomLocation, esupNfcTagLog.getEppn(), date, nomSessionEpreuve);
			}else {
				sessionLocationId = tagCheckRepository.getSessionLocationIdWithDateFin(nomLocation, esupNfcTagLog.getEppn(), date, dateFin, nomSessionEpreuve);
			}
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
		TagChecker tagChecker =  (isPresent)? tagCheckerRepository.findTagCheckerByUserAppEppnEquals(auth.getName(), null).getContent().get(0) : null;
		
    	presentTagCheck.setTagChecker(tagChecker);
    	presentTagCheck.setTagDate(date);
    	presentTagCheck.setSessionLocationBadged(sessionLocationBadged);
    	presentTagCheck.setNbBadgeage(tagCheckService.getNbBadgeage(presentTagCheck, isPresent));
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
    		dataEmitterService.sendData(presentTagCheck, percent, totalPresent, 0, sessionLocationBadged, "");
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
		SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findByNomSessionEpreuve(splitLocationNom[0], null).getContent().get(0);
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
