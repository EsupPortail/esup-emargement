package org.esupportail.emargement.web.admin;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@PreAuthorize(value="@userAppService.isAdmin()")
public class AppliConfigController {
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	LogService logService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	HelpService helpService;

	@Resource
	AppliConfigService appliConfigService;
	
	private final static String ITEM = "appliConfig";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/appliConfig")
	public String list(Model model, @RequestParam(required = false, value = "cat") String cat) {
		List<String> configs = appliConfigRepository.findAllByOrderByCategory().stream().map(a -> a.getCategory()).distinct().collect(Collectors.toList());
		String currentCat = cat== null? configs.get(0) : cat;
        model.addAttribute("cats", configs);
        model.addAttribute("currentCat", currentCat);
        model.addAttribute("appliConfigPage", appliConfigRepository.findAllByCategoryOrderByKey(currentCat));
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "admin/appliConfig/list";
	}
	
	@GetMapping(value = "/admin/appliConfig/updateConfigs")
	public String updateConfigs(@PathVariable String emargementContext, final RedirectAttributes redirectAttributes) {
		Context context = contextRepository.findByContextKey(emargementContext);
		redirectAttributes.addFlashAttribute("nbUpdate", appliConfigService.updateAppliconfig(context));
		return String.format("redirect:/%s/admin/appliConfig", emargementContext);
	}
	
	@GetMapping(value = "/admin/appliConfig/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("appliConfig",  appliConfigRepository.findById(id).get());
        return "admin/appliConfig/show";
    }
	
    @GetMapping(value = "/admin/appliConfig", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	AppliConfig appliConfig = new AppliConfig();
    	populateEditForm(uiModel, appliConfig);
        return "admin/appliConfig/create";
    }
    
    @GetMapping(value = "/admin/appliConfig/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	AppliConfig appliConfig = appliConfigRepository.findById(id).get();
    	uiModel.addAttribute("checked", appliConfig.getType().name());
    	populateEditForm(uiModel, appliConfig);
        return "admin/appliConfig/update";
    }
    
    void populateEditForm(Model uiModel, AppliConfig appliConfig) {
        uiModel.addAttribute("appliConfig", appliConfig);
    }
    
    @PostMapping("/admin/appliConfig/create")
    public String create(@PathVariable String emargementContext, @Valid AppliConfig appliConfig, BindingResult bindingResult, Model uiModel, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, appliConfig);
            return "admin/appliConfig/create";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(appliConfigRepository.countByKey(appliConfig.getKey())>0) {
        	redirectAttributes.addFlashAttribute("key",appliConfig.getKey());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, config déjà existante : " + "Key : ".concat(appliConfig.getKey()));
        	return "admin/appliConfig/list";
        }
		appliConfig.setContext(contexteService.getcurrentContext());
		appliConfigRepository.save(appliConfig);
		log.info("Création config : " + "Key : ".concat(appliConfig.getKey()));
		logService.log(ACTION.AJOUT_CONFIG, RETCODE.SUCCESS, "Key : ".concat(appliConfig.getKey()).concat(" value : ").concat(appliConfig.getValue()), auth.getName(), null,
				emargementContext, null);
		return String.format("redirect:/%s/admin/appliConfig", emargementContext);
    }
    
    @PostMapping("/admin/appliConfig/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid AppliConfig appliConfig, BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, appliConfig);
            return "admin/appliConfig/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AppliConfig original = appliConfigRepository.findById(id).get();
        original.setValue(appliConfig.getValue());
        original.setDescription(appliConfig.getDescription());
        appliConfigRepository.save(original);
        log.info("Maj config : " + "Key : ".concat(appliConfig.getKey()).concat(appliConfig.getValue()));
        logService.log(ACTION.UPDATE_CONFIG, RETCODE.SUCCESS, "Key : ".concat(appliConfig.getKey()).concat(" value : ").concat(appliConfig.getValue()), auth.getName(), null,
        		emargementContext, null);
        return String.format("redirect:/%s/admin/appliConfig?cat=%s", emargementContext, original.getCategory());
    }
    
    @PostMapping(value = "/admin/appliConfig/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, final RedirectAttributes redirectAttributes) {
    	AppliConfig appliConfig = appliConfigRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
			appliConfigRepository.delete(appliConfig);
			log.info("Suppression config : " + "Key : ".concat(appliConfig.getKey()).concat(appliConfig.getValue()));
	        logService.log(ACTION.DELETE_CONFIG, RETCODE.SUCCESS, "Key : ".concat(appliConfig.getKey()).concat(" value : ").concat(appliConfig.getValue()), auth.getName(), null,
	        		emargementContext, null);
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("appliConfig", appliConfig.getKey());
			redirectAttributes.addFlashAttribute("error", "constrainttError");
		}
    	return String.format("redirect:/%s/admin/appliConfig", emargementContext);
    }
}
