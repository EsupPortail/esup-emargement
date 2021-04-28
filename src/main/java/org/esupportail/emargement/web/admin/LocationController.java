package org.esupportail.emargement.web.admin;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.custom.LocationRepositoryCustom;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.EventService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LocationService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.StoredFileService;
import org.esupportail.emargement.utils.ToolUtil;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.fortuna.ical4j.data.ParserException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin()")
public class LocationController {
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	LocationRepositoryCustom locationRepositoryCustom;
	
	@Autowired
	CampusRepository campusRepository;
	
	@Resource
	StoredFileService storedFileService;
	
	@Resource
	LocationService locationService;
	
	@Resource
	LogService logService;
	
	@Resource
	LdapService ldapService;
	
	@Resource
	HelpService helpService;
	
	@Resource
	ContextService contexteService;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Resource
	EventService eventService;
	
	@Autowired
	ToolUtil toolUtil;
	
	private final static String ITEM = "location";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/admin/location")
	public String list(Model model, @PageableDefault(direction = Direction.ASC, sort = "nom", size = 1)  Pageable pageable, @RequestParam(value="location", required = false) String location) {
		
		Long count = locationRepository.count();
		
		int size = pageable.getPageSize();
		if( size == 1 && count>0) {
			size = count.intValue();
		}
		Page<Location> locationPage = locationRepository.findAll(toolUtil.updatePageable(pageable, size));
		if(location != null) {
			locationPage = locationRepository.findByNom(location, toolUtil.updatePageable(pageable, size));
			model.addAttribute("location", location);
			model.addAttribute("collapse", "show");
		}
        model.addAttribute("locationPage", locationPage);
        model.addAttribute("paramUrl", "0");
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("selectAll", count);
		return "admin/location/list";
	}
	
	@GetMapping(value = "/admin/location/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("location",  locationRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "admin/location/show";
    }
	
    @GetMapping(value = "/admin/location", params = "form", produces = "text/html")
    public String createForm(Model uiModel) throws IOException, ParserException {
    	Location location = new Location();
    	uiModel.addAttribute("eventLocations", locationService.getSuggestedLocation());
    	populateEditForm(uiModel, location);
        return "admin/location/create";
    }
    
    @GetMapping(value = "/admin/location/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Location location = locationRepository.findById(id).get();
    	populateEditForm(uiModel, location);
        return "admin/location/update";
    }
    
    void populateEditForm(Model uiModel, Location Location) {
    	uiModel.addAttribute("allCampuses", campusRepository.findAll());
        uiModel.addAttribute("location", Location);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
    }
    
    @PostMapping("/admin/location/create")
    public String create(@PathVariable String emargementContext, @Valid Location location, BindingResult bindingResult, Model uiModel, 
    		HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) throws IOException {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, location);
            return "admin/location/create";
        }
        uiModel.asMap().clear();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(locationRepository.countByNom(location.getNom())>0) {
        	redirectAttributes.addFlashAttribute("nom", location.getNom());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, lieu déjà existant : " + location.getNom().concat(" - ").concat(location.getCampus().getSite()));
        	return String.format("redirect:/%s/admin/location?form", emargementContext);
        }else {
        	location.setContext(contexteService.getcurrentContext());
        	locationRepository.save(location);
        	log.info("ajout d'un lieu : " + location.getNom().concat(" - ").concat(location.getCampus().getSite()));
        	logService.log(ACTION.AJOUT_LOCATION, RETCODE.SUCCESS, location.getNom().concat(" - ").concat(location.getCampus().getSite()), ldapService.getEppn(auth.getName()), null, emargementContext, null);
            return String.format("redirect:/%s/admin/location", emargementContext);
        }
    }
    
    @PostMapping("/admin/location/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid Location location, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) throws IOException {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, location);
            return "admin/location/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        List<SessionLocation> sls = sessionLocationRepository.findByLocationIdAndCapaciteGreaterThan(location.getId(), location.getCapacite());
        if(locationRepository.countByNom(location.getNom())>0 && !location.getNom().equalsIgnoreCase(locationRepository.findById(location.getId()).get().getNom())) {
        	redirectAttributes.addFlashAttribute("nom", location.getNom());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj, lieu déjà existant : " + location.getNom().concat(" - ").concat(location.getCampus().getSite()));
        	return String.format("redirect:/%s/admin/location/%s?form", emargementContext, location.getId());
        }else if(!sls.isEmpty()){
        	redirectAttributes.addFlashAttribute("capacite", location.getCapacite());
        	redirectAttributes.addFlashAttribute("sls", sls);
        	log.info("Erreur lors de la maj, la capacité de " + location.getCapacite() + " est insuffisante car plus élevée dans un lieu de session utilisé !!! ");
        	return String.format("redirect:/%s/admin/location/%s?form", emargementContext, location.getId());
        }else {
        	location.setCampus(location.getCampus());
        	location.setContext(contexteService.getcurrentContext());
        	locationRepository.save(location);
        	log.info("maj lieu : " + location.getNom().concat(" - ").concat(location.getCampus().getSite()));
        	logService.log(ACTION.UPDATE_LOCATION, RETCODE.SUCCESS, location.getNom().concat(" - ").concat(location.getCampus().getSite()), ldapService.getEppn(auth.getName()), null, emargementContext, null);
            return String.format("redirect:/%s/admin/location", emargementContext);
        }        
    }
    
    @PostMapping(value = "/admin/location/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	Location location = locationRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
    		locationRepository.delete(location);
    		log.info("Suppression du lieu : " + location.getNom().concat(" - ").concat(location.getCampus().getSite()));
    		logService.log(ACTION.DELETE_LOCATION, RETCODE.SUCCESS, location.getNom().concat(" - ").concat(location.getCampus().getSite()), ldapService.getEppn(auth.getName()), null, emargementContext, null);
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("item", location.getNom());
			redirectAttributes.addFlashAttribute("error", "constrainttError");
		}    	
        return String.format("redirect:/%s/admin/location", emargementContext);
    }
    
    @GetMapping("/admin/location/search")
    @ResponseBody
    public List<Location> search(@PathVariable String emargementContext, @RequestParam("searchValue") String searchString){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<Location> locations= locationRepositoryCustom.findAll(searchString, emargementContext);
    	
        return locations;
    }
    
    @GetMapping("/admin/location/addAll")
    @Transactional
    public String addAll(@PathVariable String emargementContext,  final RedirectAttributes redirectAttributes) throws IOException, ParserException{
		List<String> nomLocations = locationService.getSuggestedLocation();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(!nomLocations.isEmpty()) {
			for(String nom : nomLocations) {
				Location location = new Location();
				location.setNom(nom);
				location.setContext(contexteService.getcurrentContext());
				locationRepository.save(location);
			}
			logService.log(ACTION.AJOUT_LOCATION, RETCODE.SUCCESS, StringUtils.join(nomLocations, ","), ldapService.getEppn(auth.getName()), null, emargementContext, null);
			log.info("Ajout de lieux venants d'ics");
		}
		redirectAttributes.addAttribute("msgModal", "ddd");
		return String.format("redirect:/%s/admin/location", emargementContext);
    }
}
