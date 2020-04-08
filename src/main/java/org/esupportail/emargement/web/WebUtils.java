package org.esupportail.emargement.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.emargement.security.ContextUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class WebUtils {

	public static String getContext(HttpServletRequest request) {
		String path = request.getServletPath();
		if("/error".equals(path)) {
			path = (String)request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		}
		String emargementContext = path.replaceFirst("/([^/]*).*", "$1");
		return emargementContext;
	}
	
	public static List<String> availableContexts() {
		 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		 if(auth != null && auth.getPrincipal() instanceof ContextUserDetails) {
			 ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
			 return userDetails.getAvailableContexts();
		 } else {
			 return new ArrayList<String>();
		 }
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
	
	public static boolean isSuperAdmin() {
		 return hasRole("ROLE_SUPER_ADMIN");
	}
	
	public static boolean isSwitchUser() {
		 return hasRole("ROLE_PREVIOUS_ADMINISTRATOR");
	}
	
	private static boolean hasRole(String roleName) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null&& auth.getAuthorities() != null){
			if(auth.getCredentials()!=null) {
				return auth.getAuthorities().contains(new SimpleGrantedAuthority(roleName));
			}else {
				ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
				Collection<SimpleGrantedAuthority> authorities = (Collection<SimpleGrantedAuthority>) userDetails.getAuthorities();
				return authorities.contains(new SimpleGrantedAuthority(roleName));
			}
		}else {
			return false;
		}
	}
}
