package org.esupportail.emargement.web;

import java.io.IOException;

import javax.annotation.Resource;

import org.esupportail.emargement.security.ContextUserDetails;
import org.esupportail.emargement.services.LdapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
	
	@Resource
	LdapService ldapService;
	
	@Value("${app.version}")
	private String appVersion;

    @ModelAttribute("appVersion")
    public String appVersion() throws IOException {
        return appVersion;
    }
    
    @ModelAttribute("name")
    public String displayName() throws IOException {
    	String displayName = "";
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null && !"anonymousUser".equals(auth.getPrincipal())) {
			displayName = ((ContextUserDetails) auth.getPrincipal()).getDisplayName();
		}
        return displayName;
    }
    
}