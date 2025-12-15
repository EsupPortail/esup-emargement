package org.esupportail.emargement.web.supervisor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.security.ContextUserDetails;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.PreferencesService;
import org.esupportail.emargement.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class PrefsController {
	
	private final static String ITEM = "prefs";
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Resource
	HelpService helpService;
	
	@Resource
	PreferencesService preferencesService;
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/supervisor/prefs")
	public String list(Model model){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
		Map<String, Long> mapCtxs = userDetails.getAvailableContextIds();
		Map<String, Long> otherCtxs = new HashMap<>();
		otherCtxs.putAll(mapCtxs);
		LinkedHashMap<UserApp,Context> map = new LinkedHashMap<>();
		String eppn = auth.getName();
		List<Object[]> ctxs = contextRepository.findByEppn(eppn);
		for(Object[] o : ctxs) {
			Context c = contextRepository.findByKey(o[1].toString());
			UserApp userApp = userAppRepository.findByEppnContext(eppn, c.getId());
			if(c.getIsActif()) {
				map.put(userApp, c);
			}
			otherCtxs.remove(c.getKey());
		}
		
		model.addAttribute("isSuperAdmin", WebUtils.isSuperAdmin());
		model.addAttribute("map", map);
		model.addAttribute("otherCtxs", otherCtxs);
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "supervisor/prefs";
	}
	
	@PostMapping("/supervisor/prefs/updateContextPriority")
    public String updatePriorityContext(@PathVariable String emargementContext, @RequestParam("userApp")List<String> userApps) {
		
		if(!userApps.isEmpty()) {
			for(String s : userApps) {
				String [] splitUeserApps = s.split("@@");
				UserApp userApp =  userAppRepository.findById(Long.valueOf(splitUeserApps[1])).get();
				
				if(userApp!=null) {
					userApp.setContextPriority(Integer.valueOf(splitUeserApps[0]));
					userAppRepository.save(userApp);
				}
			}
		}
		 return String.format("redirect:/%s/supervisor/prefs", emargementContext);
	}
	
    @GetMapping("/supervisor/prefs/updatePrefs")
    @ResponseBody
    public void updatePrefs(@PathVariable String emargementContext, @RequestParam String pref, @RequestParam String value) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		preferencesService.updatePrefs(pref, value, eppn, emargementContext, "dummy") ;
    }
}
