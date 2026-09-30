package org.esupportail.emargement.web;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.beans.DashboardData;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.DashboardService;
import org.esupportail.emargement.services.PreferencesService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
@HelpPage("dashboard")
public class DashboardController {
	
	@Resource
	DashboardService dashboardService;
	
	@Resource
	ContextService contextService;
	
	@Resource
	PreferencesService preferencesService;
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return  "dashboard";
	}
	
	public static final String DASHBOARD_CONTEXT_KEY = "dashboardContextKey";
	public static final String DASHBOARD_MES_SESSIONS = "dashboardMesSessions";
	

	@GetMapping({"/dashboard", "/dashboard/{dashboardView}"})
	public String dashboard(
			@PathVariable String emargementContext, 
	        @PathVariable(required = false) String dashboardView,
	        @RequestParam(required = false) String contextKey,
	        @RequestParam(required = false) Boolean mesSessions,
	        @RequestParam(required = false) Boolean dashboardFilterSubmitted,
	        Model model) {

	    if (dashboardView == null) {
	        dashboardView = "today";
	    }
	    
	    String eppn = WebUtils.getEppn();

		if (contextKey == null) {
			List<Prefs> prefsContext = dashboardService.getPrefs(DASHBOARD_CONTEXT_KEY);
			if (!prefsContext.isEmpty()) {
				String value = prefsContext.get(0).getValue();
				if (!"".equals(value)) {
					contextKey = value;
				}
			}
		}

		if (mesSessions == null) {
			if (Boolean.TRUE.equals(dashboardFilterSubmitted)) {
				// Le formulaire vient d'être soumis et le switch est OFF
				mesSessions = false;
			} else {
				// Première arrivée : charger la préférence
				List<Prefs> prefsMesSessions = dashboardService.getPrefs(DASHBOARD_MES_SESSIONS);
				mesSessions = !prefsMesSessions.isEmpty() && Boolean.parseBoolean(prefsMesSessions.get(0).getValue());
			}
		}

	    if (WebUtils.isSupervisor()) {
	        mesSessions = true;
	    }

	    contextKey =  dashboardService.getContextKey(contextKey);
	    DashboardData dashboard;

	    switch (dashboardView) {
	    case "today":
	        dashboard = dashboardService.getToday(contextKey, mesSessions, emargementContext);
	        break;

	    case "week":
	        dashboard = dashboardService.getWeek(contextKey, mesSessions);
	        break;

	    case "month":
	        dashboard = dashboardService.getMonth(contextKey, mesSessions);
	        break;

	    case "year":
	        dashboard = dashboardService.getYear(contextKey, mesSessions);
	        break;

	    default:
	        throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND);
	    }
	    
		preferencesService.updatePrefs(DASHBOARD_CONTEXT_KEY, contextKey, eppn, emargementContext, "dummy");
		preferencesService.updatePrefs(DASHBOARD_MES_SESSIONS, Boolean.toString(mesSessions), eppn, emargementContext,
				"dummy");
	    
	    model.addAttribute("dashboard", dashboard);
	    model.addAttribute("dashboardView", dashboardView);
	    model.addAttribute("contextKey", contextKey);
	    model.addAttribute("contexts", contextService.findDistinctKey());
	    model.addAttribute("currentCtx", contextKey);
	    model.addAttribute("role", dashboardService.getRole());
	    model.addAttribute("mesSessions", mesSessions);
	    
	    return "home/dashboard";
	}
}
