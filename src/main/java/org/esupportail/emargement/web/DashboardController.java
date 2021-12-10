package org.esupportail.emargement.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.UserAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class DashboardController {
	
	private final static String ITEM = "dashboard";
	
	@Resource
	HelpService helpService;
	
	@Resource
	UserAppService userAppService;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Resource
	LdapService ldapService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/dashboard")
	public String list(Model model, @PageableDefault(size = 10, direction = Direction.ASC, sort = "userApp.eppn")  Pageable pageable) throws ParseException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();		
		//List<UserLdap> userLdap = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		
		List<UserLdap> userLdap = ldapService.getUserLdaps(null, auth.getName());
		
		Page<TagChecker> tagCheckerPage = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(userLdap.get(0).getEppn(), pageable);
		model.addAttribute("userLdap", userLdap.get(0));
		model.addAttribute("tagCheckerPage", tagCheckerPage);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	    Date today = new Date();
	    String strDate = formatter.format(today);
	    Date date = formatter.parse(strDate);
		//Next
		List<SessionEpreuve> seNextList = sessionEpreuveRepository.findAllByDateExamenGreaterThan(date);
		if(!seNextList.isEmpty()) {
			List<SessionLocation> slsNext =sessionLocationRepository.findSessionLocationBySessionEpreuve(seNextList.get(0), null).getContent();
			if(!slsNext.isEmpty()) {
				List<TagChecker> tcsNext = tagCheckerRepository.findTagCheckerBySessionLocationIn(slsNext, null).getContent();
				List<UserApp> userApps = tcsNext.stream().map(tc -> tc.getUserApp()).collect(Collectors.toList());
				if(!userApps.isEmpty()) {
					userAppService.setNomPrenom(userApps, true);
					model.addAttribute("userApps", userApps);
				}
				model.addAttribute("seNext", seNextList.get(0));
			}
		}
		//Today
		List<SessionEpreuve> seTodayList = sessionEpreuveRepository.findAllByDateExamenOrDateFinNotNullAndDateFinLessThanEqualAndDateFinGreaterThanEqual(date, date, date);
		if(!seTodayList.isEmpty()) {
			model.addAttribute("seTodayList", seTodayList.get(0));
			List<SessionLocation> slsToday =sessionLocationRepository.findSessionLocationBySessionEpreuve(seTodayList.get(0), null).getContent();
			if(!slsToday.isEmpty()) {
				List<TagChecker> tcsToday = tagCheckerRepository.findTagCheckerBySessionLocationIn(slsToday, null).getContent();
				List<UserApp> userAppsToday = tcsToday.stream().map(tc -> tc.getUserApp()).collect(Collectors.toList());
				if(!userAppsToday.isEmpty()) {
					userAppService.setNomPrenom(userAppsToday, true);
					model.addAttribute("userAppsToday", userAppsToday);
				}				
			}
		}
		//Previous
		List<SessionEpreuve> sePreviousList = sessionEpreuveRepository.findAllByDateExamenLessThan(date);
		if(!sePreviousList.isEmpty()) {
			List<SessionLocation> slsPrevious =sessionLocationRepository.findSessionLocationBySessionEpreuve(sePreviousList.get(0), null).getContent();
			if(!slsPrevious.isEmpty()) {
				List<TagChecker> tcsPrevious = tagCheckerRepository.findTagCheckerBySessionLocationIn(slsPrevious, null).getContent();
				List<UserApp> userAppsPrevious = tcsPrevious.stream().map(tc -> tc.getUserApp()).collect(Collectors.toList());
				if(!userAppsPrevious.isEmpty()) {
					userAppService.setNomPrenom(userAppsPrevious, true);
					model.addAttribute("userAppsPrevious", userAppsPrevious);
				}				
			}
			model.addAttribute("sePrevious", sePreviousList.get(0));
		}
		model.addAttribute("nextSessions", sessionEpreuveRepository.countByDateExamenGreaterThanEqual(new Date()));
	    model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "dashboard";
	}

}
