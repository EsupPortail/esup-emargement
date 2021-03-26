package org.esupportail.emargement.web.admin;

import javax.annotation.Resource;

import org.esupportail.emargement.services.HelpService;
import org.springframework.beans.factory.annotation.Value;
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
	
	private final static String ITEM = "apps";
	
	@Value("${emargement.esupnfctag.link.jar}")
	private String nfcTagJar;
	
	@Value("${emargement.esupnfctag.link.exe}")
	private String nfcTagExe;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/apps")
	public String list(Model model){
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("appExe", nfcTagExe);
		model.addAttribute("appJar", nfcTagJar);
		return "admin/apps";
	}

}
