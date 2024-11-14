package org.esupportail.emargement.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.emargement.security.ContextUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class WebUtils {
	
	private static final Logger log = LoggerFactory.getLogger(WebUtils.class);

	static List<String> CONTEXTS_DENIED = Arrays.asList( new String[]{"logout", "login", "resources", "webjars", "css", "js", "wsrest", "images", "favicon.ico"});

	public static String getContext(HttpServletRequest request) {
		String path = request.getServletPath();
		if("/error".equals(path)) {
			path = (String)request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
			log.warn("URL was /error - retrieve forward request uri to compute context : " + path);
		}
		String emargementContext = path.replaceFirst("/([^/]*).*", "$1");
		if(CONTEXTS_DENIED.contains(emargementContext)) {
			return "";
		}
		return emargementContext;
	}
	
	public static List<String> availableContexts() {
		 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		 if(auth != null && auth.getPrincipal() instanceof ContextUserDetails) {
			 ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
			 return userDetails.getAvailableContexts();
		 }
		return new ArrayList<>();
	}

	public static boolean isUser() {
		return hasRole("ROLE_USER");
	}
	
	public static boolean isSupervisor() {
		return hasRole("ROLE_SUPERVISOR");
	}
	
	public static boolean isManager() {
		return hasRole("ROLE_MANAGER");
	}
	
	public static boolean isSuperManager() {
		return hasRole("ROLE_SUPER_MANAGER");
	}
	
	public static boolean isAdmin() {
		 return hasRole("ROLE_ADMIN");
	}

	public static boolean isAnonymous() {
		return hasRole("ROLE_ANONYMOUS");
	}
	
	public static boolean isSuperAdmin() {
		 return hasRole("ROLE_SUPER_ADMIN");
	}
	
	public static boolean isSwitchUser() {
		 return hasRole("ROLE_PREVIOUS_ADMINISTRATOR");
	}
	
	@SuppressWarnings("unchecked")
	private static boolean hasRole(String roleName) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null&& auth.getAuthorities() != null){
			if(auth.getCredentials()!=null) {
				return auth.getAuthorities().contains(new SimpleGrantedAuthority(roleName));
			}
			ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
			if("ROLE_PREVIOUS_ADMINISTRATOR".equals(roleName)) {
				return auth.getAuthorities().toString().contains("ROLE_PREVIOUS_ADMINISTRATOR");
			}
			Collection<SimpleGrantedAuthority> authorities = (Collection<SimpleGrantedAuthority>) userDetails.getAuthorities();
			return authorities.contains(new SimpleGrantedAuthority(roleName));
		}
		return false;
	}
}
