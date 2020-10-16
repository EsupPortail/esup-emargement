package org.esupportail.emargement.web.supervisor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.PresenceService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class PresenceController {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired	
	AppliConfigService appliConfigService;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	private TagCheckRepository tagCheckRepository;
	
	@Autowired
	private TagCheckerRepository tagCheckerRepository;
	
    @Resource
    UserLdapRepository userLdapRepository;
    
    @Resource
    LocationRepository locationRepository;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Resource
	LdapService ldapService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Resource
	TagCheckService tagCheckService;

	@Resource
	PresenceService presenceService;
	
	@Resource
	SessionEpreuveService ssssionEpreuveService;
	
	@Resource
	ContextService contexteService;
	
    @Resource
    SessionEpreuveService sessionEpreuveService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "presence";
	
	private final static String SEE_OLD_SESSIONS = "seeOldSessions";
	
	@Autowired
	ToolUtil toolUtil;
	
	@Value("${emargement.wsrest.photo.prefixe}")
	private String photoPrefixe;
	
	@Value("${emargement.wsrest.photo.suffixe}")
	private String photoSuffixe;

    @GetMapping("/supervisor/presence")
    public String getListPresence(@Valid SessionEpreuve sessionEpreuve, BindingResult bindingResult, Model uiModel, 
    		@RequestParam(value ="location", required = false) Long sessionLocationId, @RequestParam(value ="present", required = false) String eppn,
    		@RequestParam(value ="sessionEpreuve" , required = false) Long sessionEpreuveId, @RequestParam(value ="tc", required = false) Long tc,
    		@PageableDefault(direction = Direction.ASC, sort = "person.eppn", size = 1)  Pageable pageable) throws JsonProcessingException {

        uiModel.asMap().clear();
        boolean isSessionLibre = false;
        boolean isCapaciteFull = false;
		Page<TagCheck> tagCheckPage = null;
		Long totalExpected = new Long(0) ;
		Long totalAll = new Long(0) ;
		Long totalPresent = new Long(0) ;
		Long totalNonRepartis = new Long(0) ;
		Long totalNotExpected = new Long(0) ;
		String currentLocation = null;
		boolean isDateOver = (sessionEpreuve.getDateExamen() != null && toolUtil.compareDate(sessionEpreuve.getDateExamen(), new Date(), "yyyy-MM-dd") < 0)? true : false;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<UserLdap> userLdap = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		String eppnAuth = (userLdap!=null)? userLdap.get(0).getEppn(): null;
        if(sessionLocationId != null) {
    		if(sessionEpreuve.isSessionEpreuveClosed) {
    			log.info("Aucun badgeage possible, la seesion " + sessionEpreuve.getNomSessionEpreuve() + " est cloturée");
    		}else {
    			totalExpected = tagCheckRepository.countBySessionLocationExpectedId(sessionLocationId);
    			totalAll = tagCheckRepository.countBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedId(sessionLocationId, sessionLocationId);
    			if(totalExpected > 0) {
    				int size = pageable.getPageSize();
    				if( size == 1) {
    					size = totalAll.intValue();
    				}
    				
		    		tagCheckPage = tagCheckService.getListTagChecksBySessionLocationId(sessionLocationId, toolUtil.updatePageable(pageable, size), eppn, true);
		    		//The list is not modifiable, obviously your client method is creating an unmodifiable list (using e.g. Collections#unmodifiableList etc.). Simply create a modifiable list before sorting:
		    		List<TagCheck> modifiableList = new ArrayList<TagCheck>(tagCheckPage.getContent());
		    		Page <TagCheck> page = new PageImpl<TagCheck>(modifiableList, toolUtil.updatePageable(pageable, size), Long.valueOf(modifiableList.size()));
		        	
		        	totalPresent = tagCheckRepository.countBySessionLocationExpectedIdAndTagDateIsNotNull(sessionLocationId);
		        	totalNonRepartis = tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNull(sessionEpreuve.getId());
		        	totalNotExpected = tagCheckRepository.countTagCheckBySessionLocationExpectedIdIsNullAndSessionLocationBadgedId(sessionLocationId);
		        	float percent = 0;
		        	if(totalExpected!=0) {
		        		percent = 100*(new Long(totalPresent).floatValue()/ new Long(totalExpected).floatValue() );
		        	}
		        	uiModel.addAttribute("percent", percent);
		        	uiModel.addAttribute("tagCheckPage", page);
    			}
    			SessionLocation sl = sessionLocationRepository.findById(sessionLocationId).get();
    			uiModel.addAttribute("sessionLocation", sl);
    			if(totalPresent>=sl.getCapacite()) {
    				isCapaciteFull = true;
    			}
	        }
    		currentLocation = sessionLocationId.toString();
    		List<TagCheck> allTagChecks = tagCheckRepository.findTagCheckBySessionLocationExpectedId(sessionLocationId);
    		tagCheckService.setNomPrenomTagChecks(allTagChecks);
    		Collections.sort(allTagChecks,  new Comparator<TagCheck>() {
    			@Override
                public int compare(TagCheck obj1, TagCheck obj2) {
    				if(obj1.getPerson().getNom() !=null && obj2.getPerson().getNom() !=null) {
    					return obj1.getPerson().getNom().compareTo(obj2.getPerson().getNom());
    				}else {
    					return obj1.getPerson().getEppn().compareTo(obj2.getPerson().getEppn());
    				}
    			}
    		});
    		uiModel.addAttribute("allTagChecks", allTagChecks);
        }
        if(tc !=null) {
        	uiModel.addAttribute("tagCheck", tagCheckRepository.findById(tc).get());
        }
		if(eppn!=null) {
			uiModel.addAttribute("eppn", eppn);
			uiModel.addAttribute("collapse", "show");
		}
		if(sessionEpreuve!=null && currentLocation != null) {
			SessionLocation sl = sessionLocationRepository.findById(Long.valueOf(currentLocation)).get();
			List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuve(sessionEpreuve);
			sls.remove(sl);
			if(!sls.isEmpty()) {
				for(SessionLocation sessionLocation : sls) {
					Long count = tagCheckRepository.countTagCheckBySessionLocationExpected(sessionLocation);
					sessionLocation.setNbInscritsSessionLocation(count);
					Long countPresent = tagCheckRepository.countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(sessionLocation);
					sessionLocation.setNbPresentsSessionLocation(countPresent);
				}
				uiModel.addAttribute("sls", sls);
			}
		}
		if(sessionEpreuve.getIsProcurationEnabled()!=null && sessionEpreuve.getIsProcurationEnabled()) {
			Long countProxyPerson = tagCheckRepository.countTagCheckBySessionEpreuveIdAndProxyPersonIsNotNull(sessionEpreuve.getId());
			uiModel.addAttribute("countProxyPerson", countProxyPerson);
	    	boolean isOver = false;
	    	if(countProxyPerson >= appliConfigService.getMaxProcurations()) {
	    		isOver = true;
	    	}
	    	uiModel.addAttribute("isOver", isOver);
	    	uiModel.addAttribute("maxProxyPerson", appliConfigService.getMaxProcurations());
		}
		if(sessionEpreuve != null ) {
			isSessionLibre = sessionEpreuve.getIsSessionLibre();
		}
		List<Prefs> prefs = prefsRepository.findByUserAppEppnAndNom(eppnAuth, SEE_OLD_SESSIONS);
		String oldSessions = (!prefs.isEmpty())? prefs.get(0).getValue() : "false";
		uiModel.addAttribute("isCapaciteFull", isCapaciteFull);
        uiModel.addAttribute("currentLocation", currentLocation);
    	uiModel.addAttribute("nbTagChecksExpected", totalExpected);
    	uiModel.addAttribute("nbTagChecksPresent", totalPresent);
    	uiModel.addAttribute("nbNonRepartis", totalNonRepartis);
    	uiModel.addAttribute("totalNotExpected", totalNotExpected);
        uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
        uiModel.addAttribute("isSessionLibre", isSessionLibre);
        
        uiModel.addAttribute("allSessionEpreuves", ssssionEpreuveService.getListSessionEpreuveByTagchecker(eppnAuth, SEE_OLD_SESSIONS));
		uiModel.addAttribute("active", ITEM);
		uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
		uiModel.addAttribute("isDateOver", isDateOver);
		uiModel.addAttribute("eppn", eppn);
		uiModel.addAttribute("selectAll", totalAll);
		uiModel.addAttribute("oldSessions", Boolean.valueOf(oldSessions));
        return "supervisor/index";
    }
    
    @GetMapping("/supervisor/sessionLocation/searchSessionLocations")
    @ResponseBody
    public List<SessionLocation> search(@RequestParam(value ="sessionEpreuve") SessionEpreuve sessionEpreuve) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<UserLdap> userLdap = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		String eppn = (userLdap != null)?  userLdap.get(0).getEppn()  : "";
		List<Prefs> prefs = prefsRepository.findByUserAppEppnAndNom(eppn, SEE_OLD_SESSIONS);
		List<SessionLocation> sessionLocationList = null;
		sessionLocationList = sessionLocationRepository.findSessionLocationBySessionEpreuve(sessionEpreuve);
    	
        return sessionLocationList;
    }
    
    @GetMapping("/supervisor/updatePresents")
    @ResponseBody
    public List<TagCheck> updatePresents(@RequestParam(value ="presence") String presence) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");

        return presenceService.updatePresents(presence) ;
    }
    
    @GetMapping("/supervisor/exportPdf")
    public void exportPdf(@PathVariable String emargementContext,HttpServletResponse response, @RequestParam(value ="sessionLocation", required = false) Long sessionLocationId, 
    		@RequestParam(value ="sessionEpreuve" , required = false) Long sessionEpreuveId) throws Exception {

    	presenceService.getPdfPresence(response, sessionLocationId, sessionEpreuveId, emargementContext);
    }
    
	@RequestMapping(value = "/supervisor/{eppn}/photo")
	@ResponseBody
	public ResponseEntity<byte[]> getPhoto(@PathVariable("eppn") String eppn) {
		
		RestTemplate template = new RestTemplate();
		String uri = null;
		byte[] photo = null;
		Boolean noPhoto = true;
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

		ResponseEntity<byte[]> httpResponse = new ResponseEntity<byte[]>(photo, headers, HttpStatus.OK);

		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();

		HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
		uri = photoPrefixe.concat(eppn).concat(photoSuffixe);
			noPhoto = false;
			httpResponse = template.exchange(uri, HttpMethod.GET, request, byte[].class);
			if(httpResponse.getBody() == null) noPhoto = true;

		if (noPhoto) {
			ClassPathResource noImg = new ClassPathResource("NoPhoto.jpg");
			try {
				photo = IOUtils.toByteArray(noImg.getInputStream());
				httpResponse = new ResponseEntity<byte[]>(photo, headers, HttpStatus.OK);
			} catch (IOException e) {
				log.info("IOException reading ", e);
			}
		}
		return httpResponse;
	}
	
    @PostMapping("/supervisor/emargementPdf")
    public void exportEamrgement(@PathVariable String emargementContext, @RequestParam("sessionLocationId") Long sessionLocationId, 
    			@RequestParam("sessionEpreuveId") Long sessionEpreuveId, @RequestParam("type") String type, HttpServletResponse response){
    	
    	sessionEpreuveService.exportEmargement(response, sessionLocationId, sessionEpreuveId, type, emargementContext);
    }
    
    @PostMapping("/supervisor/saveProcuration")
    public String saveProcuration(@PathVariable String emargementContext, @RequestParam("substituteId") Long substituteId, @RequestParam("tcId") Long id , 
    		final RedirectAttributes redirectAttributes) {
    	
    	TagCheck tc = tagCheckRepository.findById(id).get();
    	Person p  = null;
    	if(substituteId != null) {
    		p = personRepository.findById(substituteId).get();
    		tc.setTagDate(new Date());
    	}else {
    		if(tc.getProxyPerson()!=null) {
    			tc.setTagDate(null);
    		}
    	}
    	tc.setProxyPerson(p);
    	tagCheckRepository.save(tc);

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			tc.getSessionEpreuve().getId(), tc.getSessionLocationExpected().getId());
    }
    
    @GetMapping("/supervisor/updatePrefs")
    @ResponseBody
    public void updatePrefs(@PathVariable String emargementContext, @RequestParam(value ="pref") String pref, @RequestParam(value ="value") String value) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<UserLdap> userLdap = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		String eppn = (userLdap != null)?  userLdap.get(0).getEppn()  : "";
        presenceService.updatePrefs(pref, value, eppn, emargementContext) ;
    }
    
    @GetMapping("/supervisor/searchUsersLdap")
    @ResponseBody
    public List<UserLdap> searchLdap(@RequestParam("searchValue") String searchValue) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	List<UserLdap> userAppsList = new ArrayList<UserLdap>();
    	userAppsList = ldapService.search(searchValue);
    	
        return userAppsList;
    }
    
    @PostMapping("/supervisor/add")
    public String addFreeUser(@PathVariable String emargementContext, @RequestParam("slId") Long slId, @RequestParam("eppn") String eppn) {
    	
    	SessionLocation sl = sessionLocationRepository.findById(slId).get();
    	
    	presenceService.saveTagCheckSessionLibre(slId, eppn, emargementContext, sl);

    	return String.format("redirect:/%s/supervisor/presence?sessionEpreuve=%s&location=%s" , emargementContext, 
    			sl.getSessionEpreuve().getId(), slId);
    }
    		
    		
}
