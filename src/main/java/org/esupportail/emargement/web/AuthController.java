package org.esupportail.emargement.web;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.services.ContextService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {

    @Resource
    ContextService contextService;
    
    @Resource
    UserLdapRepository userLdapRepository;
	
	@GetMapping("/login")
	public String login() {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if(auth != null
				&& auth.getPrincipal() != null
				&& auth.getPrincipal() instanceof UserDetails) {
			String emargementContext = contextService.getDefaultContext();
			return "redirect:/" + emargementContext;
		}
		
	    return "redirect:/";
	}
	
	@ResponseBody
	@GetMapping("/logout")
	public String logout(
	  HttpServletRequest request, 
	  HttpServletResponse response, 
	  SecurityContextLogoutHandler logoutHandler) {
	    Authentication auth = SecurityContextHolder
	      .getContext().getAuthentication();
	    logoutHandler.logout(request, response, auth );
	    new CookieClearingLogoutHandler(
	      AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY)
	      .logout(request, response, auth);
	    return "auth/logout";
	}
	
}
