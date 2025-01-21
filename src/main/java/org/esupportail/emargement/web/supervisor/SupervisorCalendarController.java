package org.esupportail.emargement.web.supervisor;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.services.CalendarService;
import org.esupportail.emargement.services.HelpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class SupervisorCalendarController {
	
	private final static String ITEM = "calendrierSup";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	CalendarService calendarService;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Resource
	HelpService helpService;
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/supervisor/calendar")
	public String list(Model model, @RequestParam(defaultValue = "", value="eppnTagChecker") String eppnTagChecker){
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		
		return "supervisor/calendar/index";
	}
	
    @RequestMapping(value="/supervisor/calendar/events", headers = "Accept=application/json; charset=utf-8")
    @ResponseBody
    public String searchLdap(@PathVariable String emargementContext, @RequestParam("start") String start, @RequestParam("end") String end) {
    	String flexJsonString = "aucune donnée à récupérer";
		try {
			flexJsonString = calendarService.getEvents(start, end, false, "mine", null, emargementContext);
		} catch (Exception e) {
			log.warn("Impossible de récupérer les évènements calendrier du contexte " + emargementContext , e);
		}
    	return flexJsonString;
    }
}
