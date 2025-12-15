package org.esupportail.emargement.web.manager;

import javax.annotation.Resource;

import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import flexjson.JSONSerializer;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class StatsController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	StatsService statsService;
	
    @Resource
    SessionEpreuveService sessionEpreuveService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "stats";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/manager/stats")
	public String list(@PathVariable String emargementContext, Model model, @RequestParam(required = false) String anneeUniv) {
		
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("years", sessionEpreuveService.getYears(emargementContext));
		if(anneeUniv==null) {
			anneeUniv = String.valueOf(sessionEpreuveService.getCurrentanneUniv());
			
		}
		model.addAttribute("currentAnneeUniv", anneeUniv);
		
		return "manager/stats/index";
	}
	
	@GetMapping(value="manager/stats/json", headers = "Accept=application/json; charset=utf-8")
	@ResponseBody 
	public String getStats(@PathVariable String emargementContext, @RequestParam String type, @RequestParam(required=false) String param, 
			@RequestParam(required=false) String anneeUniv) {
		String flexJsonString = "Aucune statistique à récupérer";
		try {
			JSONSerializer serializer = new JSONSerializer();
			flexJsonString = serializer.deepSerialize(statsService.getStats(type, emargementContext, param, anneeUniv));
			
		} catch (Exception e) {
			log.warn("Impossible de récupérer les statistiques " + type , e);
		}
		
    	return flexJsonString;
	}
}
