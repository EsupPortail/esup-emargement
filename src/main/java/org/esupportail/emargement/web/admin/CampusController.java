package org.esupportail.emargement.web.admin;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin()")
public class CampusController {
	
	@Autowired
	CampusRepository campusRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	LogService logService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	HelpService helpService;
	
	@Autowired
	ToolUtil toolUtil;
	
	private final static String ITEM = "campus";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/campus")
	public String list(Model model, @PageableDefault(size = 1, direction = Direction.ASC, sort = "site")  Pageable pageable) {
		
		Long count = campusRepository.count();
		
		int size = pageable.getPageSize();
		if( size == 1 && count >0) {
			size = count.intValue();
		}
		
        Page<Campus> campusPage = campusRepository.findAll(toolUtil.updatePageable(pageable, size));
        model.addAttribute("campusPage", campusPage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("selectAll", count);
		return "admin/campus/list";
	}
	
	@GetMapping(value = "/admin/campus/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("campus",  campusRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "admin/campus/show";
    }
	
    @GetMapping(value = "/admin/campus", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Campus campus = new Campus();
    	populateEditForm(uiModel, campus);
        return "admin/campus/create";
    }
    
    @GetMapping(value = "/admin/campus/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Campus campus = campusRepository.findById(id).get();
    	populateEditForm(uiModel, campus);
        return "admin/campus/update";
    }
    
    void populateEditForm(Model uiModel, Campus campus) {
    	uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        uiModel.addAttribute("campus", campus);
    }
    
    @PostMapping("/admin/campus/create")
    public String create(@PathVariable String emargementContext, @Valid Campus campus, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, campus);
            return "admin/campus/create";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(campusRepository.countBySite(campus.getSite())>0) {
        	redirectAttributes.addFlashAttribute("nom", campus.getSite());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, site déjà existant : " + campus.getSite());
        	return String.format("redirect:/%s/admin/campus?form", emargementContext);
        }else {
        	campus.setContext(contexteService.getcurrentContext());
            campusRepository.save(campus);
            log.info("Ajout site : " + campus.getSite());
            logService.log(ACTION.AJOUT_CAMPUS, RETCODE.SUCCESS, "Site : ".concat(campus.getSite()), auth.getName(), null, emargementContext, null);
            return String.format("redirect:/%s/admin/campus", emargementContext);
        }
    }
    
    @PostMapping("/admin/campus/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid Campus campus, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, 
    		final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, campus);
            return "admin/campus/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(campusRepository.countBySite(campus.getSite())>0 && !campus.getSite().equalsIgnoreCase(campusRepository.findById(campus.getId()).get().getSite())) {
        	redirectAttributes.addFlashAttribute("nom", campus.getSite());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj, site déjà existant : " + campus.getSite());
        	return String.format("redirect:/%s/admin/campus/%s?form", emargementContext, campus.getId());
        }else {
        	campus.setId(id);
        	campus.setContext(contexteService.getcurrentContext());
            campusRepository.save(campus);
            log.info("Maj site : " + campus.getSite());
            logService.log(ACTION.UPDATE_CAMPUS, RETCODE.SUCCESS, "Site : ".concat(campus.getSite()), auth.getName(), null, emargementContext, null);
            return String.format("redirect:/%s/admin/campus", emargementContext);
        }  
    }
    
    @PostMapping(value = "/admin/campus/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	Campus campus = campusRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
			campusRepository.delete(campus);
			log.info("Suppression du  site  : " + campus.getSite());
			logService.log(ACTION.DELETE_CAMPUS, RETCODE.SUCCESS, "Site : ".concat(campus.getSite()), auth.getName(), null, emargementContext, null);
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("item", campus.getSite());
			redirectAttributes.addFlashAttribute("error", "constrainttError");
		}
    	return String.format("redirect:/%s/admin/campus", emargementContext);
    }
    
}
