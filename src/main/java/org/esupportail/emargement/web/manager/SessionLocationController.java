package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.SessionLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class SessionLocationController {
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Resource
	SessionLocationService sessionLocationService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "sessionLocation";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	/**
	 * @param emargementContext  
	 */
	@GetMapping(value = "/manager/sessionLocation/sessionEpreuve/{id}", produces = "text/html")
    public String listSesionLocationBySessionEpreuve(@PathVariable String emargementContext, @PathVariable("id") SessionEpreuve sessionEpreuve, Model model, 
    		@PageableDefault(size = 20, direction = Direction.ASC, sort = {"isTiersTempsOnly", "priorite"})  Pageable pageable) {

        Page<SessionLocation> sessionLocationPage = sessionLocationRepository.findSessionLocationBySessionEpreuve(sessionEpreuve, pageable);
        model.addAttribute("isSessionEpreuveClosed", sessionEpreuveService.isSessionEpreuveClosed(sessionEpreuveRepository.findById(sessionEpreuve.getId()).get()));
        model.addAttribute("sessionLocationPage", sessionLocationPage);
        model.addAttribute("sessionEpreuve", sessionEpreuveRepository.findById(sessionEpreuve.getId()).get());
		model.addAttribute("paramUrl", sessionEpreuve.getId());
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "manager/sessionLocation/list";
    }
	
	@GetMapping(value = "/manager/sessionLocation/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel) {
        uiModel.addAttribute("sessionLocation",  sessionLocationRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "manager/sessionLocation/show";
    }
	
    @GetMapping(value = "/manager/sessionLocation", params = "form", produces = "text/html")
    public String createForm(Model uiModel, @RequestParam(value = "sessionEpreuve", required = true) Long id,
    		@RequestParam(required = false) String modal) {
    	SessionLocation sessionLocation = new SessionLocation();
	    populateEditForm(uiModel, sessionLocation, id);
	    List<Integer> priorityList = sessionLocationService.getPriorityList(sessionEpreuveRepository.findById(id).get());
	    int priority = (priorityList.isEmpty())? 1 : Collections.max(priorityList)+1;
	    sessionLocation.setPriorite(priority);
	    if(modal != null){
	    	uiModel.addAttribute("sessionEpreuve", id);
        	return "manager/sessionLocation/create-modal :: modal-step2";
        }
        return "manager/sessionLocation/create";
    }
    
    @GetMapping(value = "/manager/sessionLocation/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable Long id, Model uiModel) {
    	SessionLocation sessionLocation = sessionLocationRepository.findById(id).get();
    	populateEditForm(uiModel, sessionLocation, sessionLocation.getSessionEpreuve().getId());
        return "manager/sessionLocation/update";
    }
    
    void populateEditForm(Model uiModel, SessionLocation sessionLocation, Long id) {
    	List<SessionEpreuve> allSe = new ArrayList<>();
    	SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
    	allSe.add(se);
    	if(sessionLocation.getId() == null && id !=null) {
    		List<SessionLocation> allSl = sessionLocationRepository.findSessionLocationBySessionEpreuve(se);
	    	List<Location> allLocations = locationRepository.findAll();
	    	List<Location> usedLocations = allSl.stream().map(e->e.getLocation()).collect(Collectors.toList());
	    	allLocations.removeAll(usedLocations);
	    	uiModel.addAttribute("allLocations", allLocations);
	    	Long selectedLocationId = allLocations.isEmpty() ? null : allLocations.get(0).getId();
	    	uiModel.addAttribute("selectedLocationId", selectedLocationId);
    	}
    	
    	uiModel.addAttribute("priorityList", sessionLocationService.getPriorityList(se));
    	uiModel.addAttribute("allSessionEpreuves", allSe);
    	uiModel.addAttribute("se", allSe.get(0));
        uiModel.addAttribute("sessionLocation", sessionLocation);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
    }
    
    @PostMapping("/manager/sessionLocation/create")
    public String create(@PathVariable String emargementContext, @Valid SessionLocation sessionLocation, 
    		BindingResult bindingResult, Model uiModel, @RequestHeader(value = "HX-Request", required = false) String hxRequest){
    	if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, sessionLocation, sessionLocation.getSessionEpreuve().getId());
            return "manager/sessionLocation/create";
        }
    	List<Integer> priorityList = sessionLocationService.getPriorityList(sessionLocation.getSessionEpreuve());
    	if(!priorityList.isEmpty() && priorityList.contains(sessionLocation.getPriorite())){
    		populateEditForm(uiModel, sessionLocation, sessionLocation.getSessionEpreuve().getId());
    		uiModel.addAttribute("existingPriority", "existingPriority");
    		uiModel.addAttribute("sessionLocation", sessionLocation);
    		return "manager/sessionLocation/create";
    	}
        
    	int capaciteMax = sessionLocation.getLocation().getCapacite();
        if (bindingResult.hasErrors() || sessionLocation.getCapacite() > capaciteMax  ) {
            populateEditForm(uiModel, sessionLocation, sessionLocation.getSessionEpreuve().getId());
            if(sessionLocation.getCapacite() > capaciteMax ) {
            	uiModel.addAttribute("error", capaciteMax);
            }
            return "manager/sessionLocation/create";
        }
        
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	sessionLocation.setContext(contexteService.getcurrentContext());
        sessionLocationRepository.save(sessionLocation);
        log.info("ajout d'un lieu de session; Site" + sessionLocation.getSessionEpreuve().getCampus().getSite()+ " Lieu " + sessionLocation.getLocation().getNom());
    	logService.log(ACTION.AJOUT_SESSION_LOCATION, RETCODE.SUCCESS, "Site : " + sessionLocation.getSessionEpreuve().getCampus().getSite()+ " - Lieu " + sessionLocation.getLocation().getNom(), 
    					auth.getName(), null, emargementContext, null);
	    if (hxRequest != null) {
	    	Long id = sessionLocation.getSessionEpreuve().getId();
	    	List<Integer> priorityList1 = sessionLocationService.getPriorityList(sessionLocation.getSessionEpreuve());
		    int priority = (priorityList1.isEmpty())? 1 : Collections.max(priorityList1)+1;
		    SessionLocation sessionLocation1 = new SessionLocation();
		    sessionLocation1.setPriorite(priority);
		    populateEditForm(uiModel, sessionLocation1, id);
		    SessionEpreuve se = sessionLocation.getSessionEpreuve();
		    uiModel.addAttribute("success", sessionLocation.getLocation().getNom());
		    uiModel.addAttribute("sessionEpreuve", se);
		    uiModel.addAttribute("count", sessionLocationRepository.countBySessionEpreuveId(se.getId()));
	    	return "manager/sessionLocation/create-modal :: modal-step2";
	    }
        return String.format("redirect:/%s/manager/sessionLocation/sessionEpreuve/" + sessionLocation.getSessionEpreuve().getId().toString(),emargementContext);
    }
    
    @PostMapping("/manager/sessionLocation/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable Long id, @Valid SessionLocation sessionLocation, 
    		BindingResult bindingResult, Model uiModel) {
    	
    	SessionLocation oldSl = sessionLocationRepository.findById(id).get();
    	int capaciteMax = oldSl.getLocation().getCapacite();
        if (bindingResult.hasErrors() || sessionLocation.getCapacite() > capaciteMax  ) {
            populateEditForm(uiModel, oldSl, oldSl.getSessionEpreuve().getId());
            if(sessionLocation.getCapacite() > capaciteMax ) {
            	uiModel.addAttribute("error", capaciteMax);
            }
            return "manager/sessionLocation/update";
        }
    	List<Integer> priorityList = sessionLocationService.getPriorityList(sessionLocation.getSessionEpreuve());
    	if(!priorityList.isEmpty() && priorityList.contains(sessionLocation.getPriorite()) && sessionLocation.getPriorite()!= oldSl.getPriorite()){
    		populateEditForm(uiModel, oldSl, oldSl.getSessionEpreuve().getId());
    		uiModel.addAttribute("existingPriority", "existingPriority");
    		return "manager/sessionLocation/update";
    	}
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        oldSl.setCapacite(sessionLocation.getCapacite());
        oldSl.setPriorite(sessionLocation.getPriorite());
        oldSl.setIsTiersTempsOnly(sessionLocation.getIsTiersTempsOnly());
        sessionLocationRepository.save(oldSl);
        log.info("ajout d'un lieu de session; Site" + oldSl.getSessionEpreuve().getCampus().getSite()+ " Lieu " + oldSl.getLocation().getNom());
    	logService.log(ACTION.UPDATE_SESSION_LOCATION, RETCODE.SUCCESS, "Site : " + oldSl.getSessionEpreuve().getCampus().getSite()+ " - Lieu " + oldSl.getLocation().getNom(), 
    					auth.getName(), null, emargementContext, null);
        return String.format("redirect:/%s/manager/sessionLocation/sessionEpreuve/" + oldSl.getSessionEpreuve().getId().toString(),emargementContext);
    }
    
    @PostMapping(value = "/manager/sessionLocation/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable Long id, final RedirectAttributes redirectAttributes) {
    	SessionLocation sessionLocation = sessionLocationRepository.findById(id).get();
    	String seId =  sessionLocation.getSessionEpreuve().getId().toString();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
			sessionLocationRepository.delete(sessionLocation);
	    	log.info("suppression d'un lieu de session; Site" + sessionLocation.getSessionEpreuve().getCampus().getSite()+ " Lieu " + sessionLocation.getLocation().getNom());
	    	logService.log(ACTION.DELETE_SESSION_LOCATION, RETCODE.SUCCESS, "Site : " + sessionLocation.getSessionEpreuve().getCampus().getSite()+ " - Lieu " + sessionLocation.getLocation().getNom(), 
	    					auth.getName(), null, emargementContext, null);
		} catch (Exception e) {
	    	log.error("suppression du lieu de session impossible car utilisé; Site : " + sessionLocation.getSessionEpreuve().getCampus().getSite()+ " Lieu " + sessionLocation.getLocation().getNom(),e);
	    	logService.log(ACTION.DELETE_SESSION_LOCATION, RETCODE.FAILED, "Site : " + sessionLocation.getSessionEpreuve().getCampus().getSite()+ " - Lieu " + sessionLocation.getLocation().getNom(), 
	    					auth.getName(), null, emargementContext, null);
	    	redirectAttributes.addFlashAttribute("item", sessionLocation.getLocation().getNom());
	    	redirectAttributes.addFlashAttribute("error", "constrainttError");
		}

        return String.format("redirect:/%s/manager/sessionLocation/sessionEpreuve/" + seId, emargementContext);
    }
	
    @GetMapping("/manager/sessionLocation/searchSessionLocations")
    @ResponseBody
    public List<Location> search(@RequestParam String searchValue, @RequestParam(value ="sessionEpreuve") Long sessionEpreuveId, @RequestParam boolean locationsUsed) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		HashMap <Long, List<Location>> mapSessions = sessionLocationService.getMapSessions(sessionEpreuveId, locationsUsed);
		List<Location> sessionLocationList= mapSessions.get(Long.valueOf(searchValue));
    	
        return sessionLocationList;
    }
    
    @GetMapping("/manager/sessionLocation/searchCapacite")
    @ResponseBody
    public String getCapacite(@RequestParam(value="location") Long id) {
        int capacite = locationRepository.findById(id).get().getCapacite();
        return "<input type='number' min='1' id='capacite' name='capacite' class='form-control' required='required' value='" + capacite + "'>";
    }
}
