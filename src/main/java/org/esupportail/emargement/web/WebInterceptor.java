package org.esupportail.emargement.web;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Help;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.HelpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
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

	@Autowired
	HelpService helpService;

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		if (modelAndView == null) {
			return;
		}

		String context = ContextHelper.getCurrentContext();

		Context configContext = null;

		if (!StringUtils.isEmpty(context) && !"all".equals(context) && !WebUtils.isAnonymous()) {
			configContext = contextRepository.findByContextKey(context);

			if (configContext == null) {
				log.warn("No context {} found in DB for url {}", context, request.getRequestURI());
			}
		}

		boolean isViewObject = modelAndView.getView() == null;
		boolean isRedirectView = !isViewObject && modelAndView.getView() instanceof RedirectView;
		boolean viewNameStartsWithRedirect = isViewObject
				&& modelAndView.getViewName().startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX);

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
			modelAndView.addObject("surveillantTerme", appliConfigService.getSurveillantTerm());
			modelAndView.addObject("isParticipantEnabled", appliConfigService.isParticipantDisplayed());
			modelAndView.addObject("isSessionGroupsDisplayed", appliConfigService.isSessionGroupsDisplayed());
			modelAndView.addObject("availableContexts", WebUtils.availableContexts());
			modelAndView.addObject("participantTerme", appliConfigService.getParticipantTerm());
			addHelp(modelAndView, handler);
		}
	}
	
	private void addHelp(ModelAndView modelAndView, Object handler) {

		if (!(handler instanceof HandlerMethod)) {
			return;
		}

		HandlerMethod handlerMethod = (HandlerMethod) handler;

		// On cherche d'abord une aide définie sur la méthode
		HelpPage helpPage = handlerMethod.getMethodAnnotation(HelpPage.class);

		// Sinon, on cherche une aide définie sur le Controller
		if (helpPage == null) {
			helpPage = handlerMethod.getBeanType().getAnnotation(HelpPage.class);
		}

		if (helpPage == null) {
			return;
		}

		Help help = helpService.getValueOfKey(helpPage.value());

		if (help != null) {
			modelAndView.addObject("help", help);
		}
	}
}