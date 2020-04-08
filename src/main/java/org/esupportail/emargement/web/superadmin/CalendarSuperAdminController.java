package org.esupportail.emargement.web.superadmin;

import javax.annotation.Resource;

import org.esupportail.emargement.services.CalendarService;
import org.esupportail.emargement.services.HelpService;
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


@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class CalendarSuperAdminController {
	
	private final static String ITEM = "calendrier";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	CalendarService calendarService;
	
	@Resource
	HelpService helpService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/superadmin/calendar")
	public String list(Model model, @RequestParam(defaultValue = "", value="eppnTagCheck") String eppnTagCheck, @RequestParam(defaultValue = "", value="eppnTagChecker") String eppnTagChecker){
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "superadmin/calendar/index";
	}
	
    @RequestMapping(value="/superadmin/calendar/events", headers = "Accept=application/json; charset=utf-8")
    @ResponseBody
    public String searchLdap(@PathVariable String emargementContext, @RequestParam("start") String start, @RequestParam("end") String end) {
    	
    	String flexJsonString = "aucune donnée à récupérer";
		
		try {
			flexJsonString = calendarService.getEvents(emargementContext, start, end, true);;
			
		} catch (Exception e) {
			log.warn("Impossible de récupérer les évènements calendrier du contexte " + emargementContext , e);
		}
		
    	return flexJsonString;
    }
}
