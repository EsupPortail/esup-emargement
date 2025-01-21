package org.esupportail.emargement.web.manager;

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
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class CalendarController {
	
	private final static String ITEM = "calendrier";
	
	private final static String CALENDAR_PREF = "calendarPref";
	
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
	
	@GetMapping(value = "/manager/calendar")
	public String list(Model model, @RequestParam(defaultValue = "", value="eppnTagChecker") String eppnTagChecker){
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String selectedListe = prefsRepository.findByUserAppEppnAndNom(auth.getName(), CALENDAR_PREF)
		                                      .stream()
		                                      .findFirst()
		                                      .map(Prefs::getValue)
		                                      .orElse("");
		String currentView = selectedListe.isEmpty() ? "all" : selectedListe;
		model.addAttribute(eppnTagChecker, prefsRepository.findByUserAppEppnAndNom(auth.getName(), CALENDAR_PREF));
		model.addAttribute("currentView", currentView);
		return "manager/calendar/index";
	}
	
    @RequestMapping(value="/manager/calendar/events", headers = "Accept=application/json; charset=utf-8")
    @ResponseBody
    public String searchLdap(@PathVariable String emargementContext, @RequestParam("start") String start, @RequestParam("end") String end, 
    		@RequestParam(value="view", required = false) String view) {
    	String flexJsonString = "aucune donnée à récupérer";
		try {
			flexJsonString = calendarService.getEvents(start, end, false, view, CALENDAR_PREF, emargementContext);
		} catch (Exception e) {
			log.warn("Impossible de récupérer les évènements calendrier du contexte " + emargementContext , e);
		}
    	return flexJsonString;
    }
}
