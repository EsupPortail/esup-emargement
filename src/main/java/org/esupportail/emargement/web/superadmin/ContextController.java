package org.esupportail.emargement.web.superadmin;

import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.custom.ContextRepositoryCustom;
import org.esupportail.emargement.security.ContextUserDetails;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class ContextController {

	private final static String ITEM = "context";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	ContextRepositoryCustom contextRepositoryCustom;
	
	@Resource
	LogService logService;
	
	@Resource
	HelpService helpService;
	
	@Resource
	ContextService contextService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/superadmin/context")
	public String list(Model model, @PageableDefault(size = 30, direction = Direction.ASC, sort = "key")  Pageable pageable) {
		
		Page<Context> contextPage = contextRepository.findAll(pageable);
		model.addAttribute("contextPage", contextPage);
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "superadmin/context/list";
	}
	
    @GetMapping(value = "/superadmin/context", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Context context = new Context();
    	populateEditForm(uiModel, context);
        return "superadmin/context/create";
    }
    
    @GetMapping(value = "/superadmin/context/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Context context = contextRepository.findById(id).get();
    	populateEditForm(uiModel, context);
        return "superadmin/context/update";
    }
	
    void populateEditForm(Model uiModel, Context context) {
        uiModel.addAttribute("context", context);
    }
    
    @Transactional
    @PostMapping("/superadmin/context/create")
    public String create(@PathVariable String emargementContext, @Valid Context context, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, context);
            return "superadmin/appliConfig/create";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(contextRepository.countByContextKey(context.getKey())>0) {
        	redirectAttributes.addFlashAttribute("item", context.getKey());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, context déjà existant : " + "Key : ".concat(context.getKey()));
        	return String.format("redirect:/%s/superadmin/context?form", emargementContext);
        }else {
        	String eppn = auth.getName();
        	context.setCreateur(eppn);
        	context.setDateCreation(new Date());
        	contextRepository.save(context);
        	appliConfigService.updateAppliconfig(context);
        	ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
        	userDetails.getAvailableContexts().add(context.getKey());
        	userDetails.getAvailableContextIds().put(context.getKey(),context.getId());
            log.info("Création contexte : " + "Url : ".concat(context.getKey()));
            logService.log(ACTION.AJOUT_CONTEXTE, RETCODE.SUCCESS, "Url : ".concat(context.getKey()).concat(" value : ").
            		concat(context.getKey()), eppn, null, 
            		emargementContext, null);
            return String.format("redirect:/%s/superadmin/context", emargementContext);
        }
    }
    
    @PostMapping("/superadmin/context/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid Context context, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, context);
            return "superadmin/appliConfig/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long count = contextRepository.countByContextKey(context.getKey());
        Context originaLContext = contextRepository.findById(id).get();
        if(count>0  && !context.getKey().equalsIgnoreCase(originaLContext.getKey())) {
        	redirectAttributes.addFlashAttribute("item", context.getKey());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj, context déjà existant : " + "Clé : ".concat(context.getKey()));
        	return String.format("redirect:/%s/superadmin/context/%s?form", emargementContext, id);
        }else {
        	String eppn = auth.getName();
        	context.setId(originaLContext.getId());
        	context.setCreateur(originaLContext.getCreateur());
        	context.setDateCreation(originaLContext.getDateCreation());
        	context.setDateModification(new Date());
        	contextRepository.save(context);
            log.info("Maj contexte : " + " Url : ".concat(context.getKey()));
            logService.log(ACTION.UPDATE_CONTEXTE, RETCODE.SUCCESS, "Clé : ".concat(context.getKey()).concat(" value : ").
            		concat(context.getKey()), eppn, null, 
            		emargementContext, null);
            return String.format("redirect:/%s/superadmin/context", emargementContext);
        }
    }
    
	@GetMapping(value = "/superadmin/context/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("context",  contextRepository.findById(id).get());
        return "superadmin/context/show";
    }
	
    @PostMapping(value = "/superadmin/context/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, @RequestParam("deleteContext") boolean deleteContext, Model uiModel, final RedirectAttributes redirectAttributes) {
    	Context context = contextRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if(deleteContext) {
	    	try {
	    		contextService.deleteContext(context);
	    		log.info("Suppression du contexte " + context.getKey()  + " par " + auth.getName());
				logService.log(ACTION.DELETE_CONTEXTE, RETCODE.SUCCESS, "Clé : ".concat(context.getKey()), auth.getName(), null, emargementContext, null);
				ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
	        	userDetails.getAvailableContexts().remove(context.getKey());
	        	userDetails.getAvailableContextIds().remove(context.getKey(),context.getId());
	    	} catch (Exception e) {
				log.error("Erreur Suppression contexte : " + context.getKey(), e);
				redirectAttributes.addFlashAttribute("item", context.getKey());
				redirectAttributes.addFlashAttribute("error", "constrainttError");
			}
    	}else {
    		log.info("Suppression avortée du contexte " + context.getKey()  + " par " + auth.getName());
    	}
    	return String.format("redirect:/%s/superadmin/context", emargementContext);
    }
}
