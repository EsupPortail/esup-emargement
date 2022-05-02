package org.esupportail.emargement.security;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ContextFilter  implements Filter {

	@Autowired
	UserAppRepositoryCustom userAppRepositorycustom;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String context = WebUtils.getContext(request);
		if (!context.isEmpty() && !WebUtils.isAnonymous() ) {
			ContextHelper.setCurrentContext(context);
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
			Map<String, Long> availableContextIds = userDetails.getAvailableContextIds();
			ContextHelper.setCurrenyIdContext(availableContextIds.get(context));
		} 
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
	}
}
