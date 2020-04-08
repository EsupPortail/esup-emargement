package org.esupportail.emargement.web.superadmin;

import javax.annotation.Resource;

import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.UserAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class SuController {
	
	@Resource
	HelpService helpService;
	
	@Resource
	UserAppService userAppService;
	
	private final static String ITEM = "su";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/superadmin/su")
	public String list(@PathVariable String emargementContext, Model model) {
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("users", userAppService.allUserApps());
		return "superadmin/su";
	}
	
}
