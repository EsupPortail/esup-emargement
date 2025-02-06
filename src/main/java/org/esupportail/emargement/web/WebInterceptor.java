package org.esupportail.emargement.web;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.services.AppliConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

public class WebInterceptor implements HandlerInterceptor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	AppliConfigService appliConfigService;

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
			String context = ContextHelper.getCurrentContext();
			Context configContext = null;
			if(!StringUtils.isEmpty(context) && !"all".equals(context) && !WebUtils.isAnonymous()) {
				configContext = contextRepository.findByContextKey(context);
				if(configContext==null) {
					log.warn("No context {} found in DB for url {}", context, request.getRequestURI());
				}
			}
			if (modelAndView != null) {
				boolean isViewObject = modelAndView.getView() == null;
				boolean isRedirectView = !isViewObject && modelAndView.getView() instanceof RedirectView;
				boolean viewNameStartsWithRedirect = isViewObject && modelAndView.getViewName().startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX);

				if (!isRedirectView && !viewNameStartsWithRedirect) {
					if (configContext != null) {
						modelAndView.addObject("title", configContext.getTitle());
					} else {
						modelAndView.addObject("title", "Esup-emargement");
					}
					modelAndView.addObject("eContext", context);
					modelAndView.addObject("isSuperAdmin", WebUtils.isSuperAdmin());
					modelAndView.addObject("isAdmin", WebUtils.isAdmin());
					modelAndView.addObject("isManager", WebUtils.isManager());
					modelAndView.addObject("isSupervisor", WebUtils.isSupervisor());
					modelAndView.addObject("isUser", WebUtils.isUser());
					modelAndView.addObject("isSwitchUser", WebUtils.isSwitchUser());
					modelAndView.addObject("isAdeCampusEnabled", appliConfigService.isAdeCampusEnabled());
					modelAndView.addObject("isEsupSignatureEnabled", appliConfigService.isEsupSignatureEnabled());
					modelAndView.addObject("isAdeCampusSurveillantEnabled", appliConfigService.isAdeCampusSurveillantEnabled());
					modelAndView.addObject("isCalendarEnabled", appliConfigService.isCalendarDisplayed());
					modelAndView.addObject("isImportExportEnabled", appliConfigService.isImportExportDisplayed());
					modelAndView.addObject("availableContexts", WebUtils.availableContexts());
				}
		}
	}
}

