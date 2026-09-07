package org.esupportail.emargement.web.supervisor;

import javax.annotation.Resource;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.services.CalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
@HelpPage("calendrierSup")
public class SupervisorCalendarController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	CalendarService calendarService;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return  "calendrierSup";
	}
	
	@GetMapping(value = "/supervisor/calendar")
	public String list(Model model){
		return "supervisor/calendar/index";
	}
	
    @GetMapping(value="/supervisor/calendar/events", headers = "Accept=application/json; charset=utf-8")
    @ResponseBody
    public String searchLdap(@PathVariable String emargementContext, @RequestParam String start, @RequestParam String end) {
    	String flexJsonString = "aucune donnée à récupérer";
		try {
			flexJsonString = calendarService.getEvents(start, end, false, "mine", null, emargementContext, "supervisor");
		} catch (Exception e) {
			log.warn("Impossible de récupérer les évènements calendrier du contexte " + emargementContext , e);
		}
    	return flexJsonString;
    }
}
