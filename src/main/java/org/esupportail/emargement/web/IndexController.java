package org.esupportail.emargement.web;

import java.util.Collection;

import javax.annotation.Resource;

import org.esupportail.emargement.security.ContextUserDetails;
import org.esupportail.emargement.services.ContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String ITEM = "index";

    @Resource
    ContextService contextService;
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
    @GetMapping("/")
    public String index(@RequestParam(required = false) String emargementContext, Model model) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    		if(auth != null
    				&& auth.getPrincipal() != null
    				&& auth.getPrincipal() instanceof UserDetails) {
	            if(emargementContext == null) {
	                emargementContext = contextService.getDefaultContext();
	            }
	            if(emargementContext == null || emargementContext.isEmpty()) {
	            	model.addAttribute("noContext", true);
	            	model.addAttribute("isSuperAdmin", WebUtils.isSuperAdmin());
	            	model.addAttribute("isAdmin", WebUtils.isAdmin());
					model.addAttribute("isManager", WebUtils.isManager());
					model.addAttribute("isSupervisor", WebUtils.isSupervisor());
					model.addAttribute("isSwitchUser", WebUtils.isSwitchUser());
					model.addAttribute("isUser", WebUtils.isUser());
	            	return "index";
	            }
	            return "redirect:/" + emargementContext;
    		}
    		return "index";
    }

    @SuppressWarnings("unchecked")
	@GetMapping("/{emargementContext}")
	public String emargementContext(@PathVariable String emargementContext, Model model) {
	       if(emargementContext == null || emargementContext.isEmpty()) {
	           return "redirect:/";
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null
				&& auth.getPrincipal() != null
				&& auth.getPrincipal() instanceof UserDetails) {
		    log.info(String.format("%s get '%s' context", auth.getPrincipal(), emargementContext));	
			Collection<SimpleGrantedAuthority> authorities = (Collection<SimpleGrantedAuthority>) auth.getAuthorities();
			if(auth.getCredentials()==null){
				ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
				authorities = (Collection<SimpleGrantedAuthority>) userDetails.getAuthorities();
			}
			if (authorities.contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")) && authorities.size() == 1 && "all".equals(emargementContext)) {
				return String.format("redirect:/%s/superadmin/admins", emargementContext);
			} else if (authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
				return String.format("redirect:/%s/dashboard", emargementContext);
			} else if (authorities.contains(new SimpleGrantedAuthority("ROLE_MANAGER"))) {
				return String.format("redirect:/%s/dashboard", emargementContext);
			} else if (authorities.contains(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))) {
				return String.format("redirect:/%s/dashboard", emargementContext);
			} else {
				if (authorities.contains(new SimpleGrantedAuthority("ROLE_USER"))) {
					return String.format("redirect:/%s/user", emargementContext);
				}else {
					model.addAttribute("index","index");
					return "index";
				}
			}
		}
		
	    return "redirect:/";
	}


	
}
