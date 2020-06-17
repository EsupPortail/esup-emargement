package org.esupportail.emargement.web.user;

import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor() or  @userAppService.isUser()")
public class UserController {
	
	@Autowired
	UserService userService;
	
	@Autowired
	HelpService helpService;
	
	private final static String ITEM = "user";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/user")
	public String list(Model model, @PageableDefault(size = 20, direction = Direction.DESC, sort = "sessionEpreuve.dateExamen")  Pageable pageable){
		
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("tagChecksPage", userService.getTagChecks(pageable));
		return "user/index";
	}

}
