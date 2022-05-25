package org.esupportail.emargement.web;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.config.EmargementConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.security.ContextUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

public class WebInterceptor implements HandlerInterceptor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource
	EmargementConfig config;
	
	@Autowired
	ContextRepository contextRepository;

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
			String context = ContextHelper.getCurrentContext();
			String displayName = "";
			Context configContext = null;
			if(!StringUtils.isEmpty(context) && !"all".equals(context) && !WebUtils.isAnonymous()) {
				configContext = contextRepository.findByContextKey(context);
				if(configContext==null) {
					log.warn("No context {} found in DB for url {}", context, request.getRequestURI());
				} else {
					Authentication auth = SecurityContextHolder.getContext().getAuthentication();
					if(auth != null) {
						displayName = ((ContextUserDetails) auth.getPrincipal()).getDisplayName();
					}
				}
			}
			if (modelAndView != null) {
				boolean isViewObject = modelAndView.getView() == null;
				boolean isRedirectView = !isViewObject && modelAndView.getView() instanceof RedirectView;
				boolean viewNameStartsWithRedirect = isViewObject && modelAndView.getViewName().startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX);

				if (!isRedirectView && !viewNameStartsWithRedirect) {
					if (configContext != null) {
						modelAndView.addObject("title", configContext.getTitle());
						modelAndView.addObject("htmlFooter", configContext.getPageFooter());
					} else {
						modelAndView.addObject("title", "Esup-emargement");
						modelAndView.addObject("htmlFooter", "Esup-emargement");
					}
					modelAndView.addObject("eContext", context);
					modelAndView.addObject("isSuperAdmin", WebUtils.isSuperAdmin());
					modelAndView.addObject("isAdmin", WebUtils.isAdmin());
					modelAndView.addObject("isManager", WebUtils.isManager());
					modelAndView.addObject("isSupervisor", WebUtils.isSupervisor());
					modelAndView.addObject("isUser", WebUtils.isUser());
					modelAndView.addObject("isSwitchUser", WebUtils.isSwitchUser());
					modelAndView.addObject("availableContexts", WebUtils.availableContexts());
					modelAndView.addObject("name", displayName);
				}
		}
	}
}

