package org.esupportail.emargement.web.supervisor;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.PresenceService;
import org.esupportail.emargement.web.wsrest.EsupNfcTagLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

@RequestMapping("/{emargementContext}/supervisor/searchPoll")
@Controller
public class SearchLongPollController {
	
	@Autowired
    LdapUserRepository ldapUserRepository;
	
	@Resource
	PresenceService presenceService;
	
	@Resource
	LdapService ldapService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	// Map avec en clef l'eppn de l'utilisateur manager potentiel badgeur -> pas plus d'un searchPoll par utilisateur.
	private Map<String, DeferredResult<String>> suspendedSearchPollRequests = new ConcurrentHashMap<String, DeferredResult<String>>();
	
	@RequestMapping
	@ResponseBody
	public DeferredResult<String> searchPoll(HttpServletRequest request) {
		final String authName;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		authName = ldapService.getEppn(auth.getName());
		final DeferredResult<String> searchEppn = new DeferredResult<String>(null, "");
		
		if(this.suspendedSearchPollRequests.containsKey(authName)) {
			this.suspendedSearchPollRequests.get(authName).setResult("stop");
		}
		this.suspendedSearchPollRequests.put(authName, searchEppn);
		
		searchEppn.onCompletion(new Runnable() {
			public void run() {
				synchronized (searchEppn) {
					if(searchEppn.equals(suspendedSearchPollRequests.get(authName))) {
						suspendedSearchPollRequests.remove(authName);
					}
				}
			}
		});
		
		// log.info("this.suspendedSearchPollRequests.size : " + this.suspendedSearchPollRequests.size());
		return searchEppn;
	}

	public void handleCard(EsupNfcTagLog esupNfcTagLog, String keyContext) throws ParseException {
		log.debug("handleCard : " + " for " + esupNfcTagLog.getEppn());
		if(this.suspendedSearchPollRequests.containsKey(esupNfcTagLog.getEppnInit())) {
			String result = presenceService.getHandleRedirectUrl(esupNfcTagLog, keyContext);
			this.suspendedSearchPollRequests.get(esupNfcTagLog.getEppnInit()).setResult(result);
		}
	}
}