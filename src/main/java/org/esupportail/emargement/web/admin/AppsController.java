package org.esupportail.emargement.web.admin;

import javax.annotation.Resource;

import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.HelpService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin()")
public class AppsController {
	
	@Resource
	HelpService helpService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	private final static String ITEM = "apps";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/apps")
	public String list(Model model){
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("appExe", appliConfigService.getAppDeskTopExe());
		model.addAttribute("appJar", appliConfigService.getAppDeskTopJar());
		return "admin/apps";
	}

}
