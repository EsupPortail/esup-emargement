package org.esupportail.emargement.web;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.emargement.services.AppliConfigService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
	
	@Resource
	AppliConfigService appliConfigService;
	
    @ModelAttribute
    public void handleRequest(HttpServletRequest request, Model model) {
        model.addAttribute("isAdeCampusEnabled", appliConfigService.isAdeCampusEnabled());
    }
}