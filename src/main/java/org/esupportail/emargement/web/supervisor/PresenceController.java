package org.esupportail.emargement.web.supervisor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.domain.MotifAbsence.StatutAbsence;
import org.esupportail.emargement.domain.MotifAbsence.TypeAbsence;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.AbsenceRepository;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.services.AbsenceService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.EmailService;
import org.esupportail.emargement.services.GroupeService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PreferencesService;
import org.esupportail.emargement.services.PresenceService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.SessionLocationService;
import org.esupportail.emargement.services.StoredFileService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.services.TagCheckerService;
import org.esupportail.emargement.services.UserAppService;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.zxing.WriterException;
import com.itextpdf.text.Document;

import flexjson.JSONSerializer;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class PresenceController {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	AbsenceRepository absenceRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Autowired	
	AppliConfigRepository appliConfigRepository;
	
	@Autowired	
	AppliConfigService appliConfigService;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;
	
	@Resource
	LdapService ldapService;

	@Resource
	PreferencesService preferencesService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	StoredFileService storedFileService;
	
	@Resource
	LogService logService;

	@Resource
	GroupeService groupeService;
	
	@Resource
	EmailService emailService;
	
	@Resource
	AbsenceService absenceService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	TagCheckService tagCheckService;

	@Resource
	PresenceService presenceService;
	
	@Resource
	SessionLocationService sessionLocationService; 
	
	@Resource
	ContextService contexteService;
	
    @Resource
    SessionEpreuveService sessionEpreuveService;
	
	@Resource
	HelpService helpService;
	
	@Resource
	UserAppService userAppService;
	
    @ModelAttribute("qrcodeChange")
    public Integer qrcodeChange(){
    	String qrcodeChange = appliConfigService.getQrCodeChange();
        return Integer.valueOf(qrcodeChange)*1000;
    }
	
	private final static String ITEM = "presence";
	
	private final static String SEE_OLD_SESSIONS = "seeOldSessions";
	private final static String ENABLE_WEBCAM = "enableWebcam";
	
	@Autowired
	ToolUtil toolUtil;
	
	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;
	
	@Value("${app.url}")
	private String appUrl;

    @GetMapping("/supervisor/presence")
    public ModelAndView getListPresence(@Valid SessionEpreuve sessionEpreuve, @PathVariable String emargementContext, 
    		@RequestParam(value ="location", required = false) Long sessionLocationId, @RequestParam(value ="present", required = false) Long presentId,
    		@RequestParam(required = false) Long tc, @RequestParam(required = false) String tcer, 
    		@RequestParam(required = false) String msgError, @RequestParam(required = false) Long update,
    		@RequestParam(required = false) String from){
    	ModelAndView uiModel= new ModelAndView("supervisor/list");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppnAuth = auth.getName();
    	uiModel.addObject("scrollTop", appliConfigService.isScrollTopEnabled());
    	uiModel.addObject("eppnAuth", eppnAuth);
    	if(update!=null) {
    		uiModel=  new ModelAndView("supervisor/list::search_list");
    		if(tcer!=null) {
        		TagChecker tagChecker = tagCheckerRepository.findById(update).get();
        		uiModel.addObject("tagChecker", tagChecker);	
    		}else {
        		TagCheck tagCheck = tagCheckRepository.findById(update).get();
        		uiModel.addObject("tagCheck", tagCheck);
    	    	Groupe gpe = sessionEpreuve.getBlackListGroupe();
    	    	String eppn = (tagCheck.getPerson()!=null)? tagCheck.getPerson().getEppn() : tagCheck.getGuest().getEmail();
    			boolean isBlackListed = groupeService.isBlackListed(gpe, eppn);	
    			if(isBlackListed) {
    				msgError = eppn;
    			}
    			if(!isBlackListed && BooleanUtils.isTrue(sessionEpreuve.getIsSaveInExcluded())) {
    				List <Long> idsGpe = new ArrayList<>();
    				idsGpe.add(gpe.getId());
    				groupeService.addMember(eppn,idsGpe);
    			}
    		}
    		uiModel.addObject("sessionEpreuve", sessionEpreuve);
    	}
        boolean isSessionLibre = false;
        boolean isCapaciteFull = false;
		Page<TagCheck> tagCheckPage = null;
		Long totalExpected = Long.valueOf(0) ;
		Long totalAll = Long.valueOf(0) ; 
		Long totalPresent = Long.valueOf(0) ;
		Long totalNonRepartis = Long.valueOf(0) ;
		Long totalNotExpected = Long.valueOf(0) ;
		String currentLocation = null;
		
		boolean isTodaySe = (sessionEpreuve.getDateExamen() != null && toolUtil.compareDate(sessionEpreuve.getDateExamen(), new Date(), "yyyy-MM-dd") == 0)? true : false;
		boolean isDateOver = (sessionEpreuve.getDateExamen() != null && toolUtil.compareDate(sessionEpreuve.getDateExamen(), new Date(), "yyyy-MM-dd") < 0)? true : false;
        if(sessionLocationId != null) {
    		if(sessionEpreuveService.isSessionEpreuveClosed(sessionEpreuve)) {
    			log.info("Aucun badgeage possible, la seesion " + sessionEpreuve.getNomSessionEpreuve() + " est cloturée");
    		}else{
    			totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
    			totalAll = tagCheckRepository.countBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedId(sessionLocationId, sessionLocationId);
    			if(totalExpected > 0) {
     				
		    		tagCheckPage = tagCheckService.getListTagChecksBySessionLocationId(sessionLocationId, null, presentId, true);
		        	
		        	totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
		        	totalNonRepartis = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNull(sessionEpreuve.getId());
		        	totalNotExpected = tagCheckRepository.countTagCheckBySessionLocationExpectedIdIsNullAndSessionLocationBadgedId(sessionLocationId);
		        	float percent = 0;
		        	if(totalExpected!=0) {
		        		percent = 100*(Long.valueOf(totalPresent).floatValue()/ Long.valueOf(totalExpected).floatValue() );
		        	}
		        	uiModel.addObject("percent", percent);
    			}
    			SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
    			uiModel.addObject("sessionLocation", sl);
    			if(totalPresent>=sl.getCapacite()) {
    				isCapaciteFull = true;
    			}
	        }
    		currentLocation = sessionLocationId.toString();
    		List<TagCheck> allTagChecks = tagCheckRepository.findTagCheckBySessionLocationExpectedId(sessionLocationId);
    		uiModel.addObject("allTagChecks", allTagChecks);
    		uiModel.addObject("triBadgeage", appliConfigService.isBadgeageSortAlpha());
        }
        if(tc !=null) {
        	uiModel.addObject("tc", tagCheckRepository.findById(tc).get());
        }
		if(presentId != null) {
			uiModel.addObject("eppn", presentId);
			uiModel.addObject("collapse", "show");
		}
		if(currentLocation != null) {
			SessionLocation sl = sessionLocationRepository.findById(Long.valueOf(currentLocation)).get();
			List<TagChecker> tagCheckers = tagCheckerRepository.findBySessionLocation(sl);
			tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
			uiModel.addObject("tagCheckers", tagCheckers);
			List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuve(sessionEpreuve);
			sls.remove(sl);
			if(!sls.isEmpty()) {
				for(SessionLocation sessionLocation : sls) {
					Long count = tagCheckRepository.countTagCheckBySessionLocationExpected(sessionLocation);
					sessionLocation.setNbInscritsSessionLocation(count);
					Long countPresent = tagCheckRepository.countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(sessionLocation);
					sessionLocation.setNbPresentsSessionLocation(countPresent);
				}
				uiModel.addObject("sls", sls);
			}
		}
		if(sessionEpreuve.getIsProcurationEnabled()!=null && sessionEpreuve.getIsProcurationEnabled()) {
			Long countProxyPerson = tagCheckRepository.countTagCheckBySessionEpreuveIdAndProxyPersonIsNotNull(sessionEpreuve.getId());
			uiModel.addObject("countProxyPerson", countProxyPerson);
	    	boolean isOver = false;
	    	if(countProxyPerson >= appliConfigService.getMaxProcurations()) {
	    		isOver = true;
	    	}
	    	uiModel.addObject("isOver", isOver);
	    	uiModel.addObject("maxProxyPerson", appliConfigService.getMaxProcurations());
		}
		
		isSessionLibre = (sessionEpreuve.getIsSessionLibre() == null) ? false : sessionEpreuve.getIsSessionLibre();
		
		List<Prefs> prefs = prefsRepository.findByUserAppEppnAndNom(eppnAuth, SEE_OLD_SESSIONS);
		if(from!=null) {
			preferencesService.updatePrefs(SEE_OLD_SESSIONS, "true", eppnAuth, emargementContext, "dummy") ;
		}
		List<Prefs> prefsWebCam = prefsRepository.findByUserAppEppnAndNom(eppnAuth, ENABLE_WEBCAM);
		String oldSessions = (!prefs.isEmpty())? prefs.get(0).getValue() : "false";
		String enableWebCam = (!prefsWebCam.isEmpty())? prefsWebCam.get(0).getValue() : "false";
		if(tagCheckPage != null) {
			uiModel.addObject("tagCheckPage", tagCheckPage.getContent());
		}
		uiModel.addObject("isCapaciteFull", isCapaciteFull);
        uiModel.addObject("currentLocation", currentLocation);
    	uiModel.addObject("nbTagChecksExpected", totalExpected);
    	uiModel.addObject("nbTagChecksPresent", totalPresent);
    	uiModel.addObject("nbNonRepartis", totalNonRepartis);
    	uiModel.addObject("totalNotExpected", totalNotExpected);
        uiModel.addObject("sessionEpreuve", sessionEpreuve);
        uiModel.addObject("isSessionLibre", isSessionLibre);
        uiModel.addObject("isGroupeDisplayed", sessionEpreuve.isGroupeDisplayed);
        uiModel.addObject("isQrCodeEnabled", appliConfigService.isQrCodeEnabled());
        uiModel.addObject("isUserQrCodeEnabled", appliConfigService.isUserQrCodeEnabled());
        uiModel.addObject("isSessionQrCodeEnabled", appliConfigService.isSessionQrCodeEnabled());
        uiModel.addObject("isCommunicationEnabled", appliConfigService.isCommunicationDisplayed());
        uiModel.addObject("isCardQrCodeEnabled", appliConfigService.isCardQrCodeEnabled());
		uiModel.addObject("allSessionLocations", sessionLocationService.getSessionLocationFromTagChecker(sessionEpreuve.getId(), eppnAuth));
        uiModel.addObject("allSessionEpreuves", sessionEpreuveService.getListSessionEpreuveByTagchecker(eppnAuth, SEE_OLD_SESSIONS));
		uiModel.addObject("active", ITEM);
		uiModel.addObject("help", helpService.getValueOfKey(ITEM));
		uiModel.addObject("isDateOver", isDateOver);
		uiModel.addObject("isTodaySe", isTodaySe);
		uiModel.addObject("eppn", presentId);
		uiModel.addObject("selectAll", totalAll);
		uiModel.addObject("oldSessions", Boolean.valueOf(oldSessions));
		uiModel.addObject("enableWebcam", Boolean.valueOf(enableWebCam));
		uiModel.addObject("msgError", ldapService.getPrenomNom(msgError));
		uiModel.addObject("emails",  appliConfigService.getListeGestionnaires());
		uiModel.addObject("displayTagCheckers",  appliConfigService.isTagCheckerDisplayed());
		uiModel.addObject("seId", sessionEpreuve.getId());
		uiModel.addObject("typePj", "session");
		uiModel.addObject("eppnAuth", eppnAuth);
		uiModel.addObject("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndIsTagCheckerVisibleTrueOrderByLibelle());
		
        return uiModel;
    }
    
    @GetMapping("/supervisor/sessionLocation/searchSessionLocations")
    public String search(@RequestParam SessionEpreuve sessionEpreuve, @RequestParam(value = "selectedLocation", required = false) Long selectedLocationId, Model uiModel,
    		 HttpServletResponse response) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	List<SessionLocation> sessionLocations = sessionLocationService.getSessionLocationFromTagChecker(sessionEpreuve.getId(), auth.getName());
		uiModel.addAttribute("sessionLocations", sessionLocations);
		uiModel.addAttribute("selectedLocationId", selectedLocationId);
		SessionLocation sl = sessionLocations.get(0);
        String redirectUrl = "?sessionEpreuve=" + sl.getSessionEpreuve().getId() + "&location=" + sl.getId();
        response.setHeader("HX-Redirect", redirectUrl);
 	    return "supervisor/session-locations :: options";
    }
    
    @GetMapping("/supervisor/exportPdf")
    public void exportPdf(@PathVariable String emargementContext,HttpServletResponse response, @RequestParam(value ="sessionLocation", required = false) 
    Long sessionLocationId)	throws Exception {
    	Document document = new Document();
    	presenceService.getPdfPresence(document, response, sessionLocationId, emargementContext, null);
    	document.close();
    }
    
    @GetMapping("/supervisor/exportCsv/{id}")
    public void exportTagChecks(@PathVariable String emargementContext, @PathVariable Long id, 
    		 HttpServletResponse response){
    	tagCheckService.exportTagChecks("CSV", id, response, emargementContext, null, false);
    }
    
	@RequestMapping(value = "/supervisor/{eppn}/photo")
	@ResponseBody
	public ResponseEntity<byte[]> getPhoto(@PathVariable String eppn) {
		
		RestTemplate template = new RestTemplate();
		String uri = null;
		byte[] photo = null;
		Boolean noPhoto = true;
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<byte[]> httpResponse = new ResponseEntity<>(photo, headers, HttpStatus.OK);
		if(!"inconnu".equals(eppn)) {
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
			MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<>();
			HttpEntity<Object> request = new HttpEntity<>(multipartMap, headers);
			uri = photoPrefixe.concat(eppn).concat(photoSuffixe);
				noPhoto = false;
				httpResponse = template.exchange(uri, HttpMethod.GET, request, byte[].class);
				if(httpResponse.getBody() == null) noPhoto = true;
		}
		if (noPhoto) {
			ClassPathResource noImg = new ClassPathResource("nophoto.png");
			try {
				photo = IOUtils.toByteArray(noImg.getInputStream());
				httpResponse = new ResponseEntity<>(photo, headers, HttpStatus.OK);
			} catch (IOException e) {
				log.info("IOException reading ", e);
			}
		}
		return httpResponse;
	}
	
    @GetMapping("/supervisor/emargementPdf")
    public void exportEmargement(@PathVariable String emargementContext, @RequestParam Long sessionLocationId, 
    			@RequestParam Long sessionEpreuveId, HttpServletResponse response){
    	
    	sessionEpreuveService.exportEmargement(response, sessionLocationId, sessionEpreuveId, "Liste", emargementContext);
    }
    
    @PostMapping("/supervisor/saveProcuration")
    public String saveProcuration(@PathVariable String emargementContext, @RequestParam(required = false) Long substituteId, @RequestParam("tcId") Long id) {
    	
    	TagCheck tc = tagCheckRepository.findById(id).get();
    	Person p  = null;
    	if(substituteId != null) {
    		p = personRepository.findById(substituteId).get();
    		tc.setTagDate(new Date());
    	}else {
    		if(tc.getProxyPerson()!=null) {
    			tc.setTagDate(null);
    		}
    	}
    	tc.setProxyPerson(p);
    	tagCheckRepository.save(tc);

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			tc.getSessionEpreuve().getId(), tc.getSessionLocationExpected().getId());
    }
    
    @GetMapping("/supervisor/searchEmails")
    @ResponseBody
    public String searchEmails(@RequestParam(required = false) String query){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		JSONSerializer serializer = new JSONSerializer();
		String flexJsonString = "";
		flexJsonString = serializer.deepSerialize(ldapService.searchEmails(query));
        return flexJsonString;
    }
    
    @PostMapping("/supervisor/add")
    public String addFreeUser(@PathVariable String emargementContext, @RequestParam Long slId, @RequestParam String searchString) {
    	
    	SessionLocation sl = sessionLocationRepository.findById(slId).get();
    	boolean isBlackListed = presenceService.saveTagCheckSessionLibre(slId, searchString, emargementContext, sl);
    	String msgError = (isBlackListed) ? "&msgError=" + searchString : "";
    	if(!isBlackListed && sl.getSessionEpreuve().getBlackListGroupe()!=null && BooleanUtils.isTrue(sl.getSessionEpreuve().getIsSaveInExcluded())) {
    		List <Long> idsGpe = new ArrayList<>();
    		idsGpe.add(sl.getSessionEpreuve().getBlackListGroupe().getId());
    		groupeService.addMember(searchString,idsGpe);
    	}

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" + msgError , emargementContext, 
    			sl.getSessionEpreuve().getId(), slId);
    }
    
    @Transactional
    @PostMapping(value = "/supervisor/tagCheck/{id}")
    @ResponseBody
    public Boolean delete(@PathVariable Long id) {
    	TagCheck tagCheck = tagCheckRepository.findById(id).get();
    	boolean isOk = false;
    	if(sessionEpreuveService.isSessionEpreuveClosed(tagCheck.getSessionEpreuve())) {
	        log.info("Maj de l'inscrit impossible car la session est cloturée : " + tagCheck.getPerson().getEppn());
    	}else {
    		Person person = tagCheck.getPerson();
    		tagCheckRepository.delete(tagCheck);
    		Long count = tagCheckRepository.countTagCheckByPerson(person);
    		if(count==0) {
    			personRepository.delete(person);
    		}
    		isOk = true;
    	}
    	return isOk;
    }
    
    @Transactional
    @PostMapping("/supervisor/savecomment")
    public String saveComment(@PathVariable String emargementContext, @RequestParam Long sessionEpreuveId, 
    		 @RequestParam Long sessionLocationId, String comment) {
    	SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
    	se.setComment(comment);
    	sessionEpreuveRepository.save(se);
    	log.info("Maj commentaire de la session " + se.getNomSessionEpreuve());
    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			sessionEpreuveId, sessionLocationId);
    }
    
    @Transactional
    @PostMapping("/supervisor/sendEmailPdf")
    public String sendPdfEmargement(@PathVariable String emargementContext, @RequestParam Long sessionEpreuveId, 
    		 @RequestParam Long sessionLocationId, @RequestParam(required = false) List<String> emails, @RequestParam(required = false) 
    		String courriels, HttpServletResponse response, final RedirectAttributes redirectAttributes){
    	
    	if(emails!= null && !emails.isEmpty() || courriels !=null) {
    		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
			try 
			{
    		    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");  
    		    String strDate = dateFormat.format(se.getDateExamen());  
    		    String strDateFin = (se.getDateFin() != null)? " / ".concat(dateFormat.format(se.getDateFin())) : "";  
    			String subject = "Emargement : " + se.getNomSessionEpreuve() + " - " + strDate + strDateFin; 
    			String bodyMsg = "PDF d'émargement ci-joint";
    			String fileName = se.getNomSessionEpreuve() + ".pdf";
    			String[] ccArray = {};
    			int i = 0;
    			String [] splitCourriels =  null;
	    		if(courriels!=null) {
	    			List<String> configsEmail = new ArrayList<>(appliConfigService.getListeGestionnaires());
	    			splitCourriels = courriels.split(",");
	    			for(int k=0; k<splitCourriels.length; k++) {
	    				if(!configsEmail.contains(splitCourriels[k])){
	    					configsEmail.add(splitCourriels[k]);
	    				}
	    				//emailService.sendMessageWithAttachment(splitCourriels[k], subject, bodyMsg, null, fileName, ccArray, inputStream);
	    			}
					AppliConfig appliConfig = appliConfigRepository.findAppliConfigByKey("LISTE_GESTIONNAIRES").get(0);
					appliConfig.setValue(StringUtils.join(configsEmail,","));
					appliConfigRepository.save(appliConfig);
	    		}
    			if(emails!= null && !emails.isEmpty()){
    				if(courriels!=null) {
    					emails.addAll(Arrays.asList(splitCourriels));
    				}
		    		for(String email : emails) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						Document document = new Document();
						presenceService.getPdfPresence(document, response, sessionLocationId, emargementContext, bos);
						document.close();
						byte[] bytes = bos.toByteArray();
						InputStream inputStream = new ByteArrayInputStream(bytes);
		    			emailService.sendMessageWithAttachment(email, subject, bodyMsg, null, fileName, ccArray, inputStream, true);
		    			i++;
		    			bos.close();
		    		}
    			}
				
	        	logService.log(ACTION.SEND_PDF_EXPORT, RETCODE.SUCCESS, "Nom : " + se.getNomSessionEpreuve(), auth.getName(), null, emargementContext, null);
	        	log.info("Envoi Pdf export " + se.getNomSessionEpreuve());
	        	redirectAttributes.addAttribute("nbEmails", i);
			} catch (Exception e) {
				log.error("Erreur lors de l'envoi de l'export PDF, sesioon :" +  se.getNomSessionEpreuve(), e);
			} 
    	}

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			sessionEpreuveId, sessionLocationId);
    }
	
	@RequestMapping(value = "/supervisor/qrCodeSession/{id}")
    @ResponseBody
    public String getQrCode(@PathVariable String emargementContext, @PathVariable Long id, HttpServletResponse response) throws WriterException, IOException {
		String eppn ="dummy";
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    String timestamp = Long.toString(System.currentTimeMillis() / 1000);
		Context ctx = contextRepository.findByKey(emargementContext);
		String qrCodeString = "true," + eppn + "," + id + "," + eppn + ",qrcode@@@" + timestamp + "@@@" + ctx.getId() + "@@@" + auth.getName();
		String enocdedQrCode = toolUtil.encodeToBase64(qrCodeString);
		String url = appUrl + "/" + emargementContext + "/user?scanClass=show&value=";
		InputStream inputStream = toolUtil.generateQRCodeImage(url + "qrcodeSession".concat(enocdedQrCode), 350, 350);
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        String base64Image = toolUtil.getBase64ImgFromInputStream(inputStream);
        return base64Image;
    }
	
	@RequestMapping(value = "/supervisor/qrCodePage/{id}")
	public String displayQrCodePage(@PathVariable("id") Long currentLocation,
			Model uiModel) {
		SessionLocation sessionLocation = sessionLocationRepository.findById(currentLocation).get();
		SessionEpreuve sessionEpreuve = sessionLocation.getSessionEpreuve();
		uiModel.addAttribute("currentLocation", currentLocation);
		uiModel.addAttribute("sessionLocation", sessionLocation);
		uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
		uiModel.addAttribute("active", "qrCodeSession");
		return "supervisor/qrCodeSession";
	}
	
	@PostMapping("/supervisor/tagCheck/updateComment")
    public String updateComment(@PathVariable String emargementContext, @RequestParam("idComment") TagCheck tc, String comment) {
    	tc.setComment(comment);
    	tagCheckService.save(tc, emargementContext);

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			tc.getSessionEpreuve().getId(), tc.getSessionLocationExpected().getId());
    }
	
	@Transactional
	@PostMapping("/supervisor/tagCheck/updateAbsence")
    public String updateAbsence(@PathVariable String emargementContext, @RequestParam MotifAbsence motifAbsence, @RequestParam String comment,
    		@RequestParam TagCheck tc) throws IOException {
		Absence absence = absenceService.createAbsence(tc, new Absence());
		absence.setCommentaire(comment);
		absence.setMotifAbsence(motifAbsence);
		tc.setAbsence(absence);
    	tagCheckService.save(tc, emargementContext);
    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			tc.getSessionEpreuve().getId(), tc.getSessionLocationExpected().getId());
    }
	
	@Transactional
	@PostMapping("/supervisor/tagCheck/deleteAbsence/{id}")
    public String deleteAbsence(@PathVariable String emargementContext, @PathVariable("id") TagCheck tc) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Absence absence = tc.getAbsence();
		tc.setAbsence(null);
    	tagCheckService.save(tc, emargementContext);
		List<StoredFile> files = storedFileRepository.findByAbsence(absence);
		if(!files.isEmpty()) {
			storedFileRepository.deleteAll(files);
		}
		absenceRepository.delete(absence);
    	logService.log(ACTION.DELETE_ABSENCE, RETCODE.SUCCESS, absence.getPerson().getEppn(), auth.getName(), null, emargementContext, null);

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			tc.getSessionEpreuve().getId(), tc.getSessionLocationExpected().getId());

    }
	
	@PostMapping("/supervisor/checkAll/{id}")
    public String checkAll(@PathVariable String emargementContext, @PathVariable("id") SessionLocation sl, @RequestParam String check) {
		if("true".equals(check)) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String eppn = auth.getName();
			List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionLocationExpectedId(sl.getId());
			int i =0;
			for(TagCheck tc : tcs) {
				if(tc.getTagDate()==null && tc.getAbsence()==null) {
					tc.setSessionLocationBadged(sl);
					tc.setTagDate(new Date());
					tc.setTypeEmargement(TypeEmargement.MANUAL);
					tc.setTagChecker(tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppn, null).getContent().get(0));
					tagCheckRepository.save(tc);
					i++;
				}
			}
			if(i>0) {
				log.info("Emargement par lot de " + i + " participant(s) de la salle : " + sl.getLocation().getNom() +  " de la session : " +  
					sl.getSessionEpreuve().getNomSessionEpreuve() + ", par  : " + eppn);
			}else {
				log.info("Aucun émargement effectué car tous les participants avaient déjà émargé");
			}
		}

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			sl.getSessionEpreuve().getId(), sl.getId());
    }

	@PostMapping("/supervisor/updateSecondTag")
    public String updateSecondTag(@PathVariable String emargementContext, @RequestParam("id") SessionLocation sl) {
		SessionEpreuve se = sl.getSessionEpreuve();
    	se.setIsSecondTag(se.getIsSecondTag()!=null && se.getIsSecondTag()? false : true);
    	sessionEpreuveRepository.save(se);
    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			se.getId(), sl.getId());
    }
	
    @Transactional
    @PostMapping("/supervisor/saveAttachment")
    public String saveAttachment(@PathVariable String emargementContext, @RequestParam SessionEpreuve sessionEpreuve, 
    		@RequestParam Long sessionLocationId, @RequestParam List<MultipartFile> files) throws IOException {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	sessionEpreuveService.save(sessionEpreuve, emargementContext, files);
    	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Ajout PJ : " + sessionEpreuve.getNomSessionEpreuve(), auth.getName(), null, emargementContext, null);
    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			sessionEpreuve.getId(), sessionLocationId);
    }
    
	@Transactional
	@RequestMapping(value = "/supervisor/storedFile/{id}/photo")
	public void getPhoto(@PathVariable Long id, HttpServletResponse response) throws IOException {
		storedFileService.getPhoto(id, response);
	}
	
    @Transactional
    @PostMapping("/supervisor/storedFile/delete")
    @ResponseBody
    public String  deleteStoredfile(@RequestParam("key") StoredFile storedFile){
    	return storedFileService.deleteStoredfile(storedFile);
    }
    
    @GetMapping("/supervisor/storedFile/{type}/{id}")
    @ResponseBody
    public List<StoredFile> getStoredfiles(@PathVariable String type, @PathVariable Long id){
		return storedFileService.getStoredfiles(type, id);
    }
    
    @GetMapping(value = "/supervisor/absence/motifs", produces = "text/html")
    public String search(Model uiModel, @RequestParam(required=false) String statut, @RequestParam(required=false) String type) {
    	if(statut!= null && type != null) {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndIsTagCheckerVisibleTrueAndStatutAbsenceAndTypeAbsenceOrderByLibelle(StatutAbsence.valueOf(statut), TypeAbsence.valueOf(type)));
    	}else if(statut == null && type != null) {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndIsTagCheckerVisibleTrueAndTypeAbsenceOrderByLibelle(TypeAbsence.valueOf(type)));
    	}else if(statut != null && type == null) {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndIsTagCheckerVisibleTrueAndStatutAbsenceOrderByLibelle(StatutAbsence.valueOf(statut)));
    	}else {
    		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueAndIsTagCheckerVisibleTrueOrderByLibelle());
    	}
    	return "supervisor/absence/selectMotifs";
    }
    
    @PostMapping("/supervisor/communication/pdf")
	public void getPdfConvocation(HttpServletResponse response, @RequestParam String htmltemplate) throws Exception {
    	tagCheckService.getPdfConvocation(response,htmltemplate);
	}
	
    @GetMapping("/supervisor/communication/{sessionEpreuve}/{sessionLocation}")
    @Transactional
    public String communicationForm(@PathVariable SessionEpreuve sessionEpreuve, @PathVariable SessionLocation sessionLocation, Model uiModel) {
    	uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
    	uiModel.addAttribute("sessionLocation", sessionLocation);
    	uiModel.addAttribute("isSendEmails",appliConfigService.isSendEmails());
    	uiModel.addAttribute("active", "communication");
        return "supervisor/communication";
    }
    
	@Transactional
	@PostMapping(value = "/supervisor/communication/send", produces = "text/html")
    public String sendConvocation(@PathVariable String emargementContext, @RequestParam String subject, @RequestParam String bodyMsg,
    		@RequestParam String htmltemplatePdf, @RequestParam Long seId, @RequestParam Long slId, final RedirectAttributes redirectAttributes) throws Exception {
		if(appliConfigService.isSendEmails()){
			tagCheckService.sendEmailConvocation(subject, bodyMsg, false, new ArrayList<>(), htmltemplatePdf, emargementContext, true, seId);
			redirectAttributes.addFlashAttribute("msgOk", "msgOk");
		}else {
			log.info("Envoi de mail désactivé :  ");
		}
		return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s", emargementContext, seId, slId);
    }
}
