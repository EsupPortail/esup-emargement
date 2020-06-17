package org.esupportail.emargement.web;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.config.EmargementConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.services.UserAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

public class WebInterceptor extends HandlerInterceptorAdapter {
	
	@Resource
	EmargementConfig config;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	private UserLdapRepository userLdapRepository;
	
	@Resource
	UserAppService userAppService;

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		String context = WebUtils.getContext(request);
		ContextHelper.setCurrentContext(context);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		List<UserLdap> userLdap = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		String name = (userLdap != null)?  userLdap.get(0).getPrenomNom()  : "";
		super.postHandle(request, response, handler, modelAndView);

		if(modelAndView != null) {
			boolean isViewObject = modelAndView.getView() == null;
			boolean isRedirectView = !isViewObject && modelAndView.getView() instanceof RedirectView;
			boolean viewNameStartsWithRedirect = isViewObject && modelAndView.getViewName().startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX);
	
			if (!isRedirectView && !viewNameStartsWithRedirect && context!=null && !context.isEmpty()) {
				Context configContext = contextRepository.findByContextKey(context);
				userAppService.setDateConnexion(auth.getName());
				if(configContext!=null) {
					modelAndView.addObject("title", configContext.getTitle());
					modelAndView.addObject("htmlFooter", configContext.getPageFooter());
				}else {
					modelAndView.addObject("title", "Emargement");
					modelAndView.addObject("htmlFooter", "Emargement");
				}
				modelAndView.addObject("eContext", context);
				modelAndView.addObject("isSuperAdmin", WebUtils.isSuperAdmin());
				modelAndView.addObject("isAdmin", WebUtils.isAdmin());
				modelAndView.addObject("isManager", WebUtils.isManager());
				modelAndView.addObject("isSupervisor", WebUtils.isSupervisor());
				modelAndView.addObject("isUser", WebUtils.isUser());
				modelAndView.addObject("isSwitchUser", WebUtils.isSwitchUser());
				modelAndView.addObject("availableContexts", WebUtils.availableContexts());
				modelAndView.addObject("name", name);
			}
		}

	}

}

