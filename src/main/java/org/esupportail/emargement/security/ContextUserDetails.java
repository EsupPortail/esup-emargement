package org.esupportail.emargement.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.esupportail.emargement.web.WebUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ContextUserDetails implements UserDetails {

	private static final long serialVersionUID = 1L;
	
	String username;
	
	Map<String, Set<GrantedAuthority>> contextAuthorities = new HashMap<String, Set<GrantedAuthority>>(); 
	
	final List<GrantedAuthority> defaultAuthorities = Arrays.asList(new GrantedAuthority[] {new SimpleGrantedAuthority("ROLE_NONE")});
	
	List<String> availableContexts;
	
	Map<String, Long> availableContextIds = new HashMap<String, Long>();

	public ContextUserDetails(String username, Map<String, Set<GrantedAuthority>> contextAuthorities, List<String> availableContexts, Map<String, Long> availableContextIds) {
		this.username = username;
		this.contextAuthorities = contextAuthorities;
		this.availableContexts = availableContexts;
		this.availableContextIds = availableContextIds;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
	    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
	    if (requestAttributes != null && requestAttributes instanceof ServletRequestAttributes) {
	        HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
	        String emargementContext = WebUtils.getContext(request);
	        if(request.getRequestURI().equals("/all/superadmin/impersonate")) {
	        	Set<GrantedAuthority> userAuthorities = new HashSet<GrantedAuthority>();
	        	for (Map.Entry<String, Set<GrantedAuthority>> entry : contextAuthorities.entrySet()) {
	               if(!entry.getValue().isEmpty()) {
	            	   for(GrantedAuthority ga : entry.getValue()) {
	            		   if(!userAuthorities.contains(ga)) {
	            			   userAuthorities.add(ga);
	            		   }
	            	   }
	               }
	            }
	        	
	        	return userAuthorities;
	        }
	        
	        if(contextAuthorities.get(emargementContext) != null && !contextAuthorities.get(emargementContext).isEmpty()) {
	        	return contextAuthorities.get(emargementContext);
	        }
	    }
		return defaultAuthorities;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	public List<String> getAvailableContexts() {
		return availableContexts;
	}

	public Map<String, Long> getAvailableContextIds() {
		return availableContextIds;
	}

	public void setAvailableContextIds(Map<String, Long> availableContextIds) {
		this.availableContextIds = availableContextIds;
	}

	public void setAvailableContexts(List<String> availableContexts) {
		this.availableContexts = availableContexts;
	}

}
