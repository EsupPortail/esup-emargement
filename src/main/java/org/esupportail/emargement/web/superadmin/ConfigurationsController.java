package org.esupportail.emargement.web.superadmin;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
@HelpPage("configurations")
public class ConfigurationsController {
	
	@Resource
	ContextService contextService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "configurations";
	}
	
	@GetMapping(value = "/superadmin/configurations")
	public String list(Model model, @RequestParam(required = false) String category) {
		List<String> categories = appliConfigService.findDistinctCategory();
		String currentCat = category== null? categories.get(0) : category;
		model.addAttribute("cats", appliConfigService.findDistinctCategory());
		model.addAttribute("appliConfigPage", appliConfigService.findByCategoryAndContextKeyOrderByKey(currentCat, null));
		model.addAttribute("currentCat", currentCat);
		model.addAttribute("appliConfigPage", appliConfigService.findGlobalConfigs(currentCat));
		model.addAttribute("allContexts", true);
		
		return "superadmin/configurations/index";
	}
	
	@PostMapping("/superadmin/configurations/updateAll")
	public String updateAll(@RequestParam String key, @RequestParam String value, @RequestParam String category,
			RedirectAttributes redirectAttributes) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		int count = appliConfigService.updateForAllContexts(key, value);
		log.info("Modification globale config : key={}, {} contextes", key, count);

		logService.log(ACTION.UPDATE_CONFIG, RETCODE.SUCCESS,
				"Modification globale - Key : " + key + " value : " + value + " (" + count + " contextes)",
				auth.getName(), null, "all", null);

		redirectAttributes.addFlashAttribute("success", "Configuration modifiée pour " + count + " contextes.");

		return "redirect:/all/superadmin/configurations" + "?category=" + category;
	}
	
	@PostMapping("/superadmin/configurations/update")
	public String update(@RequestParam Long id, @RequestParam String value, @RequestParam String category,
			RedirectAttributes redirectAttributes) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		AppliConfig config = appliConfigService.updateConfig(id, value);
		String contextKey = config.getContext().getKey();

		log.info("Modification config : key={}, context={}", config.getKey(), contextKey);

		logService.log(LogService.ACTION.UPDATE_CONFIG, LogService.RETCODE.SUCCESS,
				"Key : " + config.getKey() + " value : " + value, auth.getName(), null, contextKey, null);

		redirectAttributes.addFlashAttribute("success", "Configuration modifiée pour le contexte " + contextKey + ".");

		return "redirect:/all/superadmin/configurations" + "?category=" + category;
	}
}
