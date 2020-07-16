package org.esupportail.emargement.web.superadmin;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.UserAppService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class SuController {
	
	@Resource
	HelpService helpService;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	LdapService ldapService;
	
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
	
    @GetMapping("/superadmin/su/searchUsersLdap")
    @ResponseBody
    public List<UserLdap> searchLdap(@RequestParam("searchValue") String searchValue) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	List<UserLdap> userAppsList = new ArrayList<UserLdap>();
    	userAppsList = ldapService.search(searchValue);
    	
        return userAppsList;
    }
}
