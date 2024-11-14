package org.esupportail.emargement.web.superadmin;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.Resource;

import org.esupportail.emargement.beans.HttpSession;
import org.esupportail.emargement.config.ApplicationStartupListener;
import org.esupportail.emargement.security.HttpSessionsListenerService;
import org.esupportail.emargement.services.LdapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class CurrentSessionsController {

    @Autowired
    private SessionRegistry sessionRegistry;
    
	@Resource
	HttpSessionsListenerService httpSessionsListenerService;
	
	LdapService ldapService;
	
    @Autowired
    private ApplicationStartupListener applicationStartupListener;
    
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String ITEM = "sessions";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/superadmin/sessions")
	public String index(Model uiModel) {

		Map<String, HttpSession> allSessions = httpSessionsListenerService.getSessions();

		List<String> sessions = new Vector<>();
		List<Object> principals = sessionRegistry.getAllPrincipals();
		
		if (principals.isEmpty()) {
		    log.info("No active sessions.");
		}else {
			for (Object p : principals) {
				if (p instanceof UserDetails) {
					String login = ((UserDetails) p).getUsername();
					sessions.add(login);
					for(SessionInformation sessionInformation: sessionRegistry.getAllSessions(p, false)) {
						if(allSessions.containsKey(sessionInformation.getSessionId())) {
							allSessions.get(sessionInformation.getSessionId()).setLogin(login);
						}
					}
				}
			}
		}
		Instant instant = applicationStartupListener.getStartupTime().atZone(ZoneId.systemDefault()).toInstant();
		uiModel.addAttribute("allSessions", allSessions.values());
		uiModel.addAttribute("appStart", Date.from(instant));

		return "superadmin/sessions/index";
	}
}
