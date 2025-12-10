package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.EsupSignature.TypeSignature;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.GuestRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.services.AbsenceService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.EmailService;
import org.esupportail.emargement.services.EsupSignatureService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.services.UserService;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.zxing.WriterException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class TagCheckController {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;

	@Autowired
	GroupeRepository groupeRepository;

	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	GuestRepository guestRepository;
	
	@Autowired
	EsupSignatureRepository esupSignatureRepository;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Resource
	UserService userService;
	
	@Resource
	PersonService personService;   
	
	@Resource
	TagCheckService tagCheckService;
	
	@Autowired
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	HelpService helpService;
	
	@Resource
	EmailService emailService;
	
	@Resource
	EsupSignatureService esupSignatureService;
	
	@Resource
	AbsenceService absenceService;
	
	private final static String ITEM = "tagCheck";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	@Autowired
	ToolUtil toolUtil;
	
	@Value("${app.url}")
	private String appUrl;
	
	@GetMapping(value = "/manager/tagCheck/sessionEpreuve/{id}", produces = "text/html")
    public String listTagCheckBySessionEpreuve(@PathVariable Long id, Model model, 
    		@PageableDefault(size = 50, direction = Direction.ASC, sort = "person.eppn")  Pageable pageable, 
    			@RequestParam(defaultValue = "") String tempsAmenage, @RequestParam(defaultValue = "") String eppn, @RequestParam(value="repartition", required = false) 
    			Long repartitionId) throws ParseException {
		
		Long count = tagCheckService.countTagchecks(tempsAmenage, eppn, id, repartitionId);
		Long unknown = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNotNull(id);
		count = count -unknown;
		int size = pageable.getPageSize();
		if( size == 1 && count>0) {
			size = count.intValue();
		}
        Page<TagCheck> tagCheckPage = tagCheckService.getTagCheckPage(tempsAmenage, eppn, id, repartitionId, toolUtil.updatePageable(pageable, size));
        int notInLdap = tagCheckService.setNomPrenomTagChecks(tagCheckPage.getContent(), false, false);
        
		String collapse ="";
		if(!eppn.isEmpty() || repartitionId !=null || !tempsAmenage.isEmpty()) {
			collapse = "show";
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
	    String strDate = dateFormat.format(date);
	    Date now = dateFormat.parse(strDate);
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		Date dateFin = se.getDateFin();
		long countForButtonLinkOrQrCode = 0;
		if(dateFin == null || dateFin.equals(se.getDateExamen()) && dateFin.equals(date)){
			countForButtonLinkOrQrCode = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndSessionLocationBadgedIsNullAndSessionEpreuveDateExamen(id, now);
		}else {
			int check = toolUtil.compareDate(se.getDateExamen(), new Date(), "yyyy-MM-dd");
			int checkIfDateFinIsOk = -1;
			if(se.getDateFin() != null) {
				checkIfDateFinIsOk = toolUtil.compareDate(se.getDateFin(), new Date(), "yyyy-MM-dd");
			}else {
				checkIfDateFinIsOk = check;
			}
			if(check<=0 && checkIfDateFinIsOk>=0) {
				countForButtonLinkOrQrCode = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndSessionLocationBadgedIsNullAndSessionEpreuveDateExamen(id, se.getDateExamen());
			}
		}
		model.addAttribute("isSessionLibre", se.getIsSessionLibre());
        model.addAttribute("tagCheckPage", tagCheckPage);
        model.addAttribute("isSessionEpreuveClosed", sessionEpreuveService.isSessionEpreuveClosed(se));
        model.addAttribute("isGroupeDisplayed", se.isGroupeDisplayed);
		model.addAttribute("paramUrl", String.valueOf(id));
		model.addAttribute("countTagChecks", count);
		model.addAttribute("tempsAmenage",tempsAmenage);
		model.addAttribute("sessionEpreuve", sessionEpreuveRepository.findById(id).get());
		model.addAttribute("sid",id);
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("listeRepartition", tagCheckService.searchRepartition(id));
		model.addAttribute("repartitionId", repartitionId);
		model.addAttribute("eppn", eppn);
		model.addAttribute("collapse", collapse);
		model.addAttribute("isQrCodeEnabled", appliConfigService.isQrCodeEnabled());
		model.addAttribute("isLinkEmargerEnabled", appliConfigService.isSendLinkEnabled());
		model.addAttribute("countForButtonLinkOrQrCode", countForButtonLinkOrQrCode);
		model.addAttribute("countRepartition", tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNull(id) - unknown);
		model.addAttribute("countConvocations", tagCheckRepository.countTagCheckBySessionEpreuveIdAndDateEnvoiConvocationIsNull(id) - unknown);
		model.addAttribute("countUnknown", unknown);
		model.addAttribute("selectAll", count);
		model.addAttribute("notInLdap", notInLdap);
		model.addAttribute("isConvocationEnabled", appliConfigService.isConvocationEnabled());
		model.addAttribute("countsignedPdf", esupSignatureRepository.countBySessionEpreuve(se));
        return "manager/tagCheck/list";
    }
	
	@GetMapping(value = "/manager/tagCheck/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel) {
		List<TagCheck> tagChecks = new ArrayList<>();
		tagChecks.add( tagCheckRepository.findById(id).get());
		tagCheckService.setNomPrenomTagChecks(tagChecks, false, false);
        uiModel.addAttribute("tagCheck", tagChecks.get(0));
        return "manager/tagCheck/show";
    }
	
    @GetMapping(value = "/manager/tagCheck", params = "form", produces = "text/html")
    public String createForm(Model uiModel, @RequestParam Long sessionEpreuve, @RequestParam(required = false) String type, @RequestParam(required = false) String modal) {
    	TagCheck tagCheck = new TagCheck();
    	populateEditForm(uiModel, tagCheck, sessionEpreuve);
    	if(type == null) {
    		type = "interne";
    	}
    	uiModel.addAttribute("type", type);
	    if(modal != null){
	    	uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
        	return "manager/tagCheck/create-modal :: modal-step4";
        }  
        return "manager/tagCheck/create";
    }
    
    @GetMapping(value = "/manager/tagCheck/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable Long id, Model uiModel) {
    	TagCheck tagCheck = tagCheckRepository.findById(id).get();
    	uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findByIsActifTrueOrderByLibelle());
    	populateEditForm(uiModel, tagCheck, tagCheck.getSessionEpreuve().getId());
        return "manager/tagCheck/update";
    }
    
    void populateEditForm(Model uiModel, TagCheck TagCheck, Long id) {
    	List<SessionEpreuve> allSe = new ArrayList<>();
    	SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
    	List<SessionLocation>  sessionLocations = sessionLocationRepository.findSessionLocationBySessionEpreuveId(id);
    	allSe.add(se);
    	uiModel.addAttribute("id", id);
    	uiModel.addAttribute("allSessionEpreuves", allSe);
    	uiModel.addAttribute("allPersons", personRepository.findAll());
    	uiModel.addAttribute("allTagCheckers", tagCheckerRepository.findAll());
    	uiModel.addAttribute("allSessionLocations", sessionLocationRepository.findAll());
    	uiModel.addAttribute("allGroupes", groupeRepository.findAll());
    	uiModel.addAttribute("allSessionLocations", sessionLocations);
    	uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        uiModel.addAttribute("tagCheck", TagCheck);
        Map<String,String> mapEtapes = new HashMap<>();
        List<String> etapes = tagCheckService.findDistinctCodeEtapeSessionEpreuve(id);
        if(!etapes.isEmpty()) {
        	for(String etape : etapes) {
        		String splitEtapes []= etape.split(" - ");
        		mapEtapes.put(splitEtapes[0], splitEtapes[1]);
        	}
        }
        uiModel.addAttribute("codeEtapes", mapEtapes);
    }
    
    @PostMapping("/manager/tagCheck/create")
    @Transactional
    public String create(@PathVariable String emargementContext, @Valid TagCheck tagCheck, BindingResult bindingResult, Model uiModel,  
    		final RedirectAttributes redirectAttributes) throws Exception {
    	
    	boolean isOk = true;
    	if(tagCheck.getSessionLocationExpected()!= null && !tagCheckService.checkImportIntoSessionLocations(tagCheck.getSessionLocationExpected().getId(), 1)) {
    		isOk = false;
    	}
        if (bindingResult.hasErrors()|| !isOk) {
            populateEditForm(uiModel, tagCheck, tagCheck.getSessionEpreuve().getId());
            if(!isOk) {
            	uiModel.addAttribute("capaciteOver", tagCheck.getSessionLocationExpected().getLocation().getNom());
            }
            return "manager/tagCheck/create";
        }
        uiModel.asMap().clear();
        List<List<String>> finalList = tagCheckService.setAddList(tagCheck);
        Map<String,String> mapTempEtapes = new HashMap <>();
        if(tagCheck.getCodeEtape()!=null) {
        	mapTempEtapes.put("addUser", tagCheck.getCodeEtape());
        }
    	List<Integer> bilanCsv =  tagCheckService.importTagCheckCsv(null, finalList, tagCheck.getSessionEpreuve().getId(), emargementContext, mapTempEtapes, 
    			tagCheck.getCheckLdap(), (tagCheck.getSessionLocationExpected() != null)?  tagCheck.getSessionLocationExpected().getId() : null, tagCheck);
    	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
    	return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/%s", emargementContext, tagCheck.getSessionEpreuve().getId());
    }
    
    @Transactional
    @PostMapping("/manager/tagCheck/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable Long id, @Valid TagCheck tagCheck, @RequestParam(required =false) MotifAbsence motifAbsence, 
    		BindingResult bindingResult, Model uiModel) throws IOException {
    	
    	boolean isOk = true;
    	TagCheck tc = tagCheckRepository.findById(id).get();
    	if(tc.getSessionLocationExpected()!= tagCheck.getSessionLocationExpected() && tagCheck.getSessionLocationExpected()!= null && !tagCheckService.checkImportIntoSessionLocations(tagCheck.getSessionLocationExpected().getId(), 1)) {
    		isOk = false;
    	}    	
        if (bindingResult.hasErrors() || !isOk) {
            populateEditForm(uiModel, tc, tc.getSessionEpreuve().getId());
            if(!isOk) {
            	uiModel.addAttribute("capaciteOver", tagCheck.getSessionLocationExpected().getLocation().getNom());
            }            
            return "manager/tagCheck/update";
        }
        uiModel.asMap().clear();
       
    	if(sessionEpreuveService.isSessionEpreuveClosed(tc.getSessionEpreuve())) {
	        log.info("Maj de l'inscrit impossible car la session est cloturée : " + tagCheck.getPerson().getEppn());
    	}else {
    		tc.setContext(contexteService.getcurrentContext());
    		tc.setIsTiersTemps(tagCheck.getIsTiersTemps());
    		tc.setComment(tagCheck.getComment());
    		tc.setSessionLocationExpected(tagCheck.getSessionLocationExpected());
    		if(motifAbsence!=null) {
    			Absence absence = new Absence();
    			absence.setMotifAbsence(motifAbsence);
    			tc.setAbsence(absenceService.createAbsence(tc, absence));
    		}else {
    			if(tc.getAbsence() != null) {
	    			absenceService.deleteAbsence(tc.getAbsence());
	    			tc.setAbsence(null);
    			}
    		}
    		tagCheckService.save(tc, emargementContext);
    	}
        return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/" + tc.getSessionEpreuve().getId(), emargementContext);
    }
    
    @Transactional
    @PostMapping(value = "/manager/tagCheck/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable Long id) {
    	TagCheck tagCheck = tagCheckRepository.findById(id).get();
    	if("CLOSED".equals(tagCheck.getSessionEpreuve().getStatutSession().getKey())) {
	        log.info("Maj de l'inscrit impossible car la session est cloturée : " + tagCheck.getPerson().getEppn());
    	}else {
    		List<EsupSignature> list = esupSignatureRepository.findByTagCheck(tagCheck);
    		if(!list.isEmpty()) {
    			esupSignatureRepository.deleteAll(list);
    		}
    		tagCheckRepository.delete(tagCheck);
    		personService.deleteUnusedPersons(contextRepository.findByContextKey(emargementContext));
    	}
        return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/" + tagCheck.getSessionEpreuve().getId(), emargementContext);
    }
    
	@Transactional
	@GetMapping(value = "/manager/tagCheck/deleteAllTagChecks/{id}", produces = "text/html")
    public String deleteRepartition(@PathVariable String emargementContext, @PathVariable Long id) {
		tagCheckService.deleteAllTagChecksBySessionEpreuveId(id);
        return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
	
    @PostMapping("/manager/tagCheck/convocationForm")
    @Transactional
    public String convocationForm(@PathVariable String emargementContext, @RequestParam(required = false) List<Long> listeIds, @RequestParam(value = "sessionEpreuveId") SessionEpreuve sessionEpreuve, 
    		@RequestParam String submit, Model uiModel, final RedirectAttributes redirectAttributes) {
    	
    	
    	if("selected".equals(submit) && listeIds!= null && listeIds.isEmpty()) {
    		 redirectAttributes.addFlashAttribute("msgModal", "Vous n'avez pas sélectionné d'inscrits");
    		 return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/%s", emargementContext, sessionEpreuve.getId());
    		
    	}
    	uiModel.addAttribute("listeIds", listeIds);
    	uiModel.addAttribute("all", ("all".equals(submit)) ? true : false);
    	uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
    	uiModel.addAttribute("tagChecks", tagCheckService.snTagChecks(listeIds));
    	uiModel.addAttribute("isSendEmails",appliConfigService.isSendEmails());
    	uiModel.addAttribute("convocationHtml", appliConfigService.getConvocationContenu());
    	uiModel.addAttribute("sujetMailConvocation", appliConfigService.getConvocationSujetMail());
    	uiModel.addAttribute("bodyMailConvocation", appliConfigService.getConvocationBodyMail());
    	uiModel.addAttribute("help", helpService.getValueOfKey("convocation"));
        return "manager/tagCheck/convocation";
    }
    
    @PostMapping("/manager/tagCheck/pdfConvocation")
	public void getPdfConvocation(HttpServletResponse response, @RequestParam String htmltemplate) throws Exception {
    	tagCheckService.getPdfConvocation(response,htmltemplate);
	}
    
    @GetMapping("/manager/tagCheck/export")
    public void exportTagChecks(@PathVariable String emargementContext,@RequestParam String type, @RequestParam("sessionId") Long id, 
    		 HttpServletResponse response){
    	tagCheckService.exportTagChecks(type, id, response, emargementContext, null, false);
    }
    
	@Transactional
	@PostMapping(value = "/manager/tagCheck/sendConvocation", produces = "text/html")
    public String sendConvocation(@PathVariable String emargementContext, @RequestParam String subject, @RequestParam String bodyMsg, 
    		@RequestParam(defaultValue = "false") boolean isSendToManager,  @RequestParam(value="all", defaultValue = "false") boolean isAll,
    		@RequestParam(defaultValue = "") List<Long> listeIds,  @RequestParam String htmltemplatePdf, @RequestParam 
    		Long seId) throws Exception {

		if(appliConfigService.isSendEmails()){
			tagCheckService.sendEmailConvocation(subject, bodyMsg, isSendToManager, listeIds, htmltemplatePdf, emargementContext, isAll, seId, true);
		}else {
			log.info("Envoi de mail désactivé :  ");
		}
		
		return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
	
    @GetMapping("/manager/tagCheck/searchUsersLdap")
    @ResponseBody
    public List<LdapUser> searchLdap(@RequestParam String searchValue) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	List<LdapUser> userAppsList = new ArrayList<>();
    	userAppsList = ldapService.search(searchValue);
    	
        return userAppsList;
    }
    
    @PostMapping("/manager/tagCheck/sendLinkOrQrCode")
    public String sendLinkOrQrCode(@PathVariable String emargementContext, @RequestParam Long seId,  @RequestParam(required = false) String population, 
    		@RequestParam String type, final RedirectAttributes redirectAttributes) throws MessagingException, IOException {
    	//On pourra rechercher par type ...
    	List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(seId);
    	if(!tcs.isEmpty()) {
    		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        	String eppnAuth = (auth!=null) ?  auth.getName() : null;
    		int nbMailEnvoye = 0;
    		int nbMailNonEnvoye = 0;
    		Boolean isSuccess = true;
    		try {
    			List<String> tagCheckList = tcs.stream().filter(tc->tc.getPerson()!=null)
    					.map(tagCheck -> tagCheck.getPerson().getEppn()).collect(Collectors.toList());
    			Map<String, LdapUser> mapTagCheckLdapUsers = ldapService.getLdapUsersFromNumList(tagCheckList,
    					"eduPersonPrincipalName");
				for(TagCheck tc : tcs) {
					String mailAdresse =  null;
					String eppn = "";
					String nomPrenom = "";
					String cc [] = {};
					if("qrCode".equals(type)) {
						if("nonext".equals(population)) {
							if(tc.getPerson() != null) {
								eppn = tc.getPerson().getEppn();
								LdapUser user = mapTagCheckLdapUsers.get(eppn);
								if(user!=null) {
									nomPrenom = user.getPrenomNom();
									mailAdresse = user.getEmail();
								}
							}
						}if("ext".equals(population)) {
							if(tc.getGuest() != null) {
								mailAdresse = tc.getGuest().getEmail();
								nomPrenom =  StringUtils.capitalize(tc.getGuest().getPrenom()) + " " + StringUtils.capitalize(tc.getGuest().getNom());
							}
						}
						else if("all".equals(population)) {
							if(tc.getPerson() != null) {
								eppn = tc.getPerson().getEppn();
								LdapUser user = mapTagCheckLdapUsers.get(eppn);
								if(user!=null) {
									mailAdresse = user.getEmail();
									nomPrenom = user.getPrenomNom();
								}
							}else if(tc.getGuest() != null) {
								mailAdresse = tc.getGuest().getEmail();
								nomPrenom =  StringUtils.capitalize(tc.getGuest().getPrenom()) + " " + StringUtils.capitalize(tc.getGuest().getNom());
							}
						}
						String slId = (tc.getSessionLocationExpected()!= null)? tc.getSessionLocationExpected().getId().toString() : null;
						if(mailAdresse != null && slId != null && !mailAdresse.isEmpty() && tc.getSessionLocationBadged()== null) {
							String nomSessionEpreuve = tc.getSessionEpreuve().getNomSessionEpreuve().replace(" ", "_");
							String pattern = "dd_MM_yyyy";
							DateFormat df = new SimpleDateFormat(pattern);
							String dateSe = df.format(tc.getSessionEpreuve().getDateExamen());
							String fileName = dateSe + "_" + nomSessionEpreuve + "_" + mailAdresse +  ".png";
							String body = appliConfigService.getQrCodebodyMail();
							body = body.replaceAll("@@nom@@", nomPrenom);
							body = body.replaceAll("@@session@@", tc.getSessionEpreuve().getNomSessionEpreuve());
							String qrCodeString = "true," + eppn + "," + slId + "," + mailAdresse + ",qrcode@@@notime@@@" + tc.getContext().getId();
							String enocdedQrCode = toolUtil.encodeToBase64(qrCodeString);
							InputStream inputStream = toolUtil.generateQRCodeImage("qrcode".concat(enocdedQrCode), 350, 350);
							emailService.sendMessageWithAttachment(mailAdresse, appliConfigService.getQrCodeSujetMail(), body, null, fileName, cc,  inputStream, true);
							nbMailEnvoye++;
						}else {
							if( tc.getSessionLocationBadged()!= null) {
								log.info("qrCode non envoyé car ... a déjà badgé");
							}
							nbMailNonEnvoye++;
						}

					}else if("link".equals(type)) {
						if(tc.getPerson() != null) {
							TagChecker tcer = tagCheckerRepository.findBySessionLocationAndUserAppEppnEquals(tc.getSessionLocationExpected(), eppnAuth);
							if(tcer !=null) {
								eppn = tc.getPerson().getEppn();
								String token =  userService.generateSessionToken(tcer.getId().toString());
								String subject = appliConfigService.getLinkSujetEmailEmarger();
								String link = appUrl + "/" + emargementContext + "/user?sessionToken=" + token;
								String body = appliConfigService.getLinkEmailEmarger();
								subject = subject.replaceAll("@@session@@", tc.getSessionEpreuve().getNomSessionEpreuve());
								LdapUser user = mapTagCheckLdapUsers.get(eppn);
								if(user!=null) {
									nomPrenom = user.getPrenomNom();
								}
								if(tc.getSessionLocationBadged()== null) {
									tc.setSessionToken(token);
									tagCheckRepository.save(tc);
									body = body.replaceAll("@@nom@@", nomPrenom);
									body = body.replaceAll("@@session@@", tc.getSessionEpreuve().getNomSessionEpreuve());
									body = body.replaceAll("@@link@@", link);
									emailService.sendSimpleMessage(user.getEmail(), subject, body, cc);
									nbMailEnvoye++;
								}else {
									if( tc.getSessionLocationBadged()!= null) {
										log.info("lien non envoyé car ... a déjà badgé");
									}
									nbMailNonEnvoye++;
								}
							}else {
								redirectAttributes.addFlashAttribute("isNotTagchecker", "isNotTagchecker");
								isSuccess = false;
							}
						}
					}
				}
				if("qrCode".equals(type)) {
					logService.log(ACTION.SEND_QRCODE, RETCODE.SUCCESS, "Nb envoyé : " + nbMailEnvoye +  " - Nb non envoyé : " + nbMailNonEnvoye , null,
							null, emargementContext, null);
					log.info("Envoi de qrCode : "  + " Nb envoyé : " + nbMailEnvoye +  " - Nb non envoyé : " + nbMailNonEnvoye);
				}else if("link".equals(type)) {
					 log.info("envoi du lien de téléchargement réussi");
					 logService.log(ACTION.SEND_LINK, RETCODE.SUCCESS, " Nombre " + nbMailEnvoye, "", null, emargementContext, null);
				}
				
			} catch (WriterException e) {
				isSuccess = false;
				if("qrCode".equals(type)) {
					logService.log(ACTION.SEND_QRCODE, RETCODE.FAILED, "Nb envoyé : " + nbMailEnvoye +  " - Nb non envoyé : " + nbMailNonEnvoye , null,
							null, emargementContext, null);
					log.error("Problàme lors de l'envoi de qrCode : "  + " Nb envoyé : " + nbMailEnvoye +  " - Nb non envoyé : " + nbMailNonEnvoye, e);
				}else if("link".equals(type)) {
					 log.error("Problème lors de l'envoi du lien de téléchargement réussi", e);
					 logService.log(ACTION.SEND_LINK, RETCODE.FAILED, " Nombre " + nbMailEnvoye, "", null, emargementContext, null);
				}
			}
    		redirectAttributes.addFlashAttribute("isSuccess", isSuccess);
    	}
    	return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/" + seId.toString(), emargementContext);
    }
    
	@GetMapping(value = "/manager/tagCheck/esupsignature/{id}", produces = "text/html")
    public String sendPdfToEsupToWorkflow(@PathVariable Long id, @PathVariable String emargementContext, HttpServletResponse response) {
		esupSignatureService.sendPdfToEsupToWorkflow(emargementContext, id, response, TypeSignature.SESSION);
		return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/%s", emargementContext, id);
	}
}
