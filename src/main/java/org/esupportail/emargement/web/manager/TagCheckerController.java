package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.SessionLocationService;
import org.esupportail.emargement.services.TagCheckerService;
import org.esupportail.emargement.services.UserAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class TagCheckerController {
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	SessionLocationService sessionLocationService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	LogService logService;
	
	@Resource
	ContextService contexteService;

	@Resource
	LdapService ldapService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "tagChecker";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/tagChecker/sessionEpreuve/{id}", produces = "text/html")
    public String listTagCheckerBySessionEpreuve(@PathVariable("id") SessionEpreuve sessionEpreuve, Model model, 
    		@PageableDefault(size = 20, direction = Direction.ASC, sort = "userApp")  Pageable pageable) {

		Page<TagChecker> tagCheckerPage = tagCheckerService.getListTagCheckerBySessionEpreuve(sessionEpreuve, pageable);
		model.addAttribute("isSessionEpreuveClosed", sessionEpreuveService.isSessionEpreuveClosed(sessionEpreuveRepository.findById(sessionEpreuve.getId()).get()));
        model.addAttribute("tagCheckerPage", tagCheckerPage);
		model.addAttribute("paramUrl", sessionEpreuve.getId());
		model.addAttribute("sessionEpreuve", sessionEpreuveRepository.findById(sessionEpreuve.getId()).get());
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("isConsignesEnabled", appliConfigService.isConsignesEnabled());
        return "manager/tagChecker/list";
    }
	
	@GetMapping(value = "/manager/tagChecker/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
		List<TagChecker> tagCheckers = new ArrayList<TagChecker>();
		tagCheckers.add( tagCheckerRepository.findById(id).get());
		tagCheckerService.setNomPrenom4TagCheckers(tagCheckers);
		
        uiModel.addAttribute("tagChecker", tagCheckers.get(0));
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "manager/tagChecker/show";
    }
	
    @GetMapping(value = "/manager/tagChecker", params = "form", produces = "text/html")
    public String createForm(Model uiModel, @RequestParam(value = "sessionEpreuve", required = false) Long id) {
    	
    	TagChecker tagChecker = new TagChecker();
    	if(id !=null) {
    		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
    		List<SessionLocation> allSl = sessionLocationRepository.findSessionLocationBySessionEpreuve(se);
    		List<UserApp> allUserApps = userAppRepository.findByContext(se.getContext());
    		uiModel.addAttribute("allSessionLocations", allSl);
    		allUserApps = userAppService.setNomPrenom(allUserApps, false);
    		allUserApps.sort(Comparator.comparing(UserApp::getNom));
    		uiModel.addAttribute("allUserApps", allUserApps);
    		
    		List<TagChecker> tcs = tagCheckerRepository.findAll();
    		List<TagChecker> distinctTagCheckers = tcs.stream()
    			    .filter(tc -> tc.getUserApp() != null && tc.getUserApp().getEppn() != null)
    			    .collect(Collectors.toMap(
    			        tc -> tc.getUserApp().getEppn(),
    			        tc -> tc,
    			        (existing, replacement) -> existing
    			    ))
    			    .values()
    			    .stream()
    			    .sorted(Comparator.comparing(tc -> tc.getUserApp().getEppn())) // Sort by eppn
    			    .collect(Collectors.toList());
    		uiModel.addAttribute("tcs", distinctTagCheckers);
    		
    	}
    	populateEditForm(uiModel, tagChecker, id);
        return "manager/tagChecker/create";
    }
    
    @GetMapping("/manager/tagChecker/usedTagCheckers")
    @ResponseBody
    public List<TagChecker> search(@RequestParam("location") Long idLocation) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		SessionLocation sl = sessionLocationRepository.findById(idLocation).get();
		List<TagChecker> allTcUsed = tagCheckerRepository.findBySessionLocation(sl);
    	
        return allTcUsed;
    }
    
    void populateEditForm(Model uiModel, TagChecker TagChecker, Long id) {
    	List<SessionEpreuve> allSe = new ArrayList<SessionEpreuve>();
    	SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
    	allSe.add(se);
    	uiModel.addAttribute("allSessionEpreuves", allSe);
        uiModel.addAttribute("tagChecker", TagChecker);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
    }
    
    @PostMapping("/manager/tagChecker/create")
    public String create(@PathVariable String emargementContext, @Valid TagChecker tagChecker, @RequestParam(value="users", required = false) List<Long> users, 
    		@RequestParam("sessionEpreuve") Long sessionEpreuve, @RequestParam(value ="allLocations", required = false) String allLocations, BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, tagChecker, tagChecker.getSessionLocation().getSessionEpreuve().getId());
            return "manager/tagChecker/create";
        }
        uiModel.asMap().clear();
        if(users != null && !users.isEmpty()){
        	SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuve).get();
        	List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuve(se);
        	for(Long id : users) {
        		UserApp userApp = userAppRepository.findById(id).get();
        		if(userApp!=null) {
    				if(allLocations == null){
    					Long count = tagCheckerRepository.countBySessionLocationAndUserApp(tagChecker.getSessionLocation(), userApp);
    					if(count == 0) {
		        			TagChecker tc = new TagChecker();
		        			tc.setUserApp(userApp);
		        			tc.setContext(contexteService.getcurrentContext());
		        			tc.setSessionLocation(tagChecker.getSessionLocation());
		        			tagCheckerRepository.save(tc);
		        			log.info("ajout surveillant : " + tc.getUserApp().getEppn());
		        			logService.log(ACTION.AJOUT_SURVEILLANT, RETCODE.SUCCESS, tc.getUserApp().getEppn(), tc.getUserApp().getEppn(), null, emargementContext, null);
    					}
    				}else {
    					if(!sls.isEmpty()) {
    						for(SessionLocation sl : sls) {
    							Long count = tagCheckerRepository.countBySessionLocationAndUserApp(sl, userApp);
    							if(count == 0) {
	    							TagChecker tc = new TagChecker();
	    		        			tc.setUserApp(userApp);
	    		        			tc.setContext(contexteService.getcurrentContext());
	    		        			tc.setSessionLocation(sl);
	    		        			tagCheckerRepository.save(tc);
	    		        			log.info("ajout surveillant : " + tc.getUserApp().getEppn());
	    		        			logService.log(ACTION.AJOUT_SURVEILLANT, RETCODE.SUCCESS, tc.getUserApp().getEppn(), tc.getUserApp().getEppn(), null, emargementContext, null);
    							}
    						}
    					}
    				}
        		}
        	}
        }
        
        return String.format("redirect:/%s/manager/tagChecker/sessionEpreuve/" + tagChecker.getSessionEpreuve().getId().toString(), emargementContext);
    }
    
    @PostMapping(value = "/manager/tagChecker/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, final RedirectAttributes redirectAttributes) {
    	TagChecker tagChecker = tagCheckerRepository.findById(id).get();
    	String seId = tagChecker.getSessionEpreuve().getId().toString();
    	if(sessionEpreuveService.isSessionEpreuveClosed(tagChecker.getSessionEpreuve())) {
	        log.info("suppression du surveillant impossible car la session est cloturée : " + tagChecker.getUserApp().getEppn());
	        logService.log(ACTION.DELETE_SURVEILLANT, RETCODE.FAILED, tagChecker.getUserApp().getEppn(), tagChecker.getUserApp().getEppn(), null, emargementContext, null);			
    	}else {
	    	try {
				tagCheckerRepository.delete(tagChecker);
		        log.info("suppression surveillant : " + tagChecker.getUserApp().getEppn());
		        logService.log(ACTION.DELETE_SURVEILLANT, RETCODE.SUCCESS, tagChecker.getUserApp().getEppn(), tagChecker.getUserApp().getEppn(), null, emargementContext, null);
			} catch (Exception e) {
		        log.info("suppression du surveillant impossible car utilisé : " + tagChecker.getUserApp().getEppn(), e);
		        logService.log(ACTION.DELETE_SURVEILLANT, RETCODE.FAILED, tagChecker.getUserApp().getEppn(), tagChecker.getUserApp().getEppn(), null, emargementContext, null);
		    	redirectAttributes.addFlashAttribute("item", tagChecker.getUserApp().getEppn());
		    	redirectAttributes.addFlashAttribute("error", "constrainttError");
			}
    	}
        return String.format("redirect:/%s/manager/tagChecker/sessionEpreuve/" + seId, emargementContext);
    }
	
    @GetMapping("/manager/tagChecker/searchSessionLocations")
    @ResponseBody
    public List<SessionLocation> search(@RequestParam("searchValue") String searchValue, @RequestParam(value ="sessionEpreuve") Long sessionEpreuveId, @RequestParam(value ="locationsUsed") boolean locationsUsed) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		HashMap <Long, List<SessionLocation>> mapSessions = sessionLocationService.getMapSessionLocations(sessionEpreuveId, locationsUsed);
		List<SessionLocation> sessionLocationList= mapSessions.get(Long.valueOf(searchValue));
    	
        return sessionLocationList;
    }
    
    @GetMapping(value = "/manager/tagChecker/consignes")
    public String getConsignes(Model uiModel, @RequestParam("seid") SessionEpreuve sessionEpreuve) {
    	uiModel.addAttribute("seid",  sessionEpreuve.getId());
    	uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
    	uiModel.addAttribute("consignesHtml", appliConfigService.getConsigneType());
    	uiModel.addAttribute("sujetMailConsignes", appliConfigService.getConsigneSujetMail());
    	uiModel.addAttribute("bodyMailConsignes", appliConfigService.getConsigneBodyMail());
    	uiModel.addAttribute("tagCheckers", tagCheckerService.getSnTagCheckers(sessionEpreuve));
    	uiModel.addAttribute("help", helpService.getValueOfKey("consignes"));
    	uiModel.addAttribute("isSendEmails",appliConfigService.isSendEmails());
        return "manager/tagChecker/consignes";
    }
    
	@Transactional
	@PostMapping(value = "/manager/tagChecker/sendConsignes", produces = "text/html")
    public String sendConvocation(@PathVariable String emargementContext, @RequestParam("subject") String subject, @RequestParam("bodyMsg") String bodyMsg, 
    		@RequestParam(value = "sessionEpreuveId") Long sessionEpreuveId,  @RequestParam("htmltemplatePdf") String htmltemplatePdf) throws Exception {
		
		if(appliConfigService.isSendEmails()){
			tagCheckerService.sendEmailConsignes(subject, bodyMsg, sessionEpreuveId, htmltemplatePdf);
		}else {
			log.info("Envoi de mail désactivé :  ");
		}
		
		return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
}
