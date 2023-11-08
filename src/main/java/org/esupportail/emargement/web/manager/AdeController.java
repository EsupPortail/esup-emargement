package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.AdeResourceBean;
import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.AdeService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PreferencesService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xml.sax.SAXException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class AdeController {
	
	private final static String [] catArray = {"category5", "category6", "category7", "category8"};
	
	private final static String ADE_STORED_COMPOSANTE = "adeStoredComposante";
	
	private final static String ADE_STORED_SALLE = "adeStoredSalle";
	
	private final static String ADE_STORED_PROJET = "adeStoredProjet";
	
	private final static String ITEM = "adeCampus";
	
	@Resource
	HelpService helpService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${emargement.ade.home.url}")
	private String urlHomeAde;
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@ModelAttribute("help")
	public String getHelp() {
		return helpService.getValueOfKey(ITEM);
	}
	
	@ModelAttribute("adeHomeUrl")
	public String getAdeHomeUrl() {
		return urlHomeAde;
	}
	
	@Resource
	AdeService adeService;
	
	@Resource
	LogService logService;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	AppliConfigRepository appliConfigRepository;

	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	CampusRepository campusRepository;
	
	@Autowired	
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	TypeSessionRepository typeSessionRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	LdapUserRepository ldapUserRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
    @Resource 
    PreferencesService preferencesService;
    
    @Resource 
    SessionEpreuveService sessionEpreuveService;
    
    @Resource
    AppliConfigService appliConfigService;
    
    @Resource 
    LdapService ldapService;
    
	//TO DO : spinner + logs + duplication nom
	
	@GetMapping(value = "/manager/adeCampus")
	public String index(@PathVariable String emargementContext, Model uiModel) throws ParseException{
	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			String sessionId = adeService.getSessionId(false, emargementContext);
			List<Prefs> prefsAdeStored = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_PROJET);
			if(prefsAdeStored.isEmpty()) {
				uiModel.addAttribute("noProject","noProject");
				uiModel.addAttribute("type", "composante");
				uiModel.addAttribute("mapComposantes", adeService.getMapComposantes(sessionId));
				uiModel.addAttribute("mapSalles", adeService.getClassroomsList(sessionId));
				uiModel.addAttribute("mapProjets", adeService.getProjectLists(sessionId));
				uiModel.addAttribute("valuesComposantes", getValuesPref(auth.getName(), ADE_STORED_COMPOSANTE));
				uiModel.addAttribute("valuesSalles", getValuesPref(auth.getName(), ADE_STORED_SALLE));
			}else {
				String idProject = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_PROJET).get(0).getValue();
				if(adeService.getConnectionProject(idProject, sessionId)==null) {
					sessionId = adeService.getSessionId(true, emargementContext);
					adeService.getConnectionProject(idProject, sessionId);
					log.info("Récupération du projet Ade " + idProject);
				}
				uiModel.addAttribute("values", getValuesPref(auth.getName(), ADE_STORED_COMPOSANTE));
				uiModel.addAttribute("existingSe", true);
			}
	
		} catch (Exception e) {
			log.error("Erreur lors de la récupération des évènements", e);
		}
		return "manager/adeCampus/index";
	}
	
	@GetMapping(value = "/manager/adeCampus/Events")
	public String getTableEvents(@PathVariable String emargementContext, Model uiModel, 
			@RequestParam(value="existingSe", required = false) String existingSe,
			@RequestParam(value="idList", required = false) List<String> idList,
			@RequestParam(value="codeComposante", required = false) String codeComposante, 
			@RequestParam(value="strDateMin", required = false) String strDateMin,
			@RequestParam(value="strDateMax", required = false) String strDateMax) throws ParseException{
	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			String sessionId = adeService.getSessionId(false, emargementContext);
			List<Prefs> prefsAdeStored = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_PROJET);
			if(prefsAdeStored.isEmpty()) {
				uiModel.addAttribute("noProject","noProject");
				uiModel.addAttribute("type", "composante");
				uiModel.addAttribute("currentComposante", codeComposante);			
				uiModel.addAttribute("mapComposantes", adeService.getMapComposantes(sessionId));
				uiModel.addAttribute("mapSalles", adeService.getClassroomsList(sessionId));
				uiModel.addAttribute("mapProjets", adeService.getProjectLists(sessionId));
				uiModel.addAttribute("valuesComposantes", getValuesPref(auth.getName(), ADE_STORED_COMPOSANTE));
				uiModel.addAttribute("valuesSalles", getValuesPref(auth.getName(), ADE_STORED_SALLE));
			}else {
				String idProject = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_PROJET).get(0).getValue();
				if(adeService.getConnectionProject(idProject, sessionId)==null) {
					sessionId = adeService.getSessionId(true, emargementContext);
					adeService.getConnectionProject(idProject, sessionId);
					log.info("Récupération du projet Ade " + idProject);
				}
				uiModel.addAttribute("currentComposante", codeComposante);
				if("myEvents".equals(codeComposante) || idList.size()>0) {
					uiModel.addAttribute("listEvents", adeService.getAdeBeans(sessionId, strDateMin, strDateMax, null, existingSe, codeComposante, idList));
				}
				uiModel.addAttribute("strDateMin", strDateMin);
				uiModel.addAttribute("strDateMax", strDateMax);
				uiModel.addAttribute("existingSe", (existingSe!=null)? true : false);
				uiModel.addAttribute("codeComposante", codeComposante);
				uiModel.addAttribute("campuses", campusRepository.findAll());
				uiModel.addAttribute("values", getValuesPref(auth.getName(), ADE_STORED_COMPOSANTE));
			}
	
		} catch (Exception e) {
			log.error("Erreur lors de la récupération des évènements", e);
		}
		return "manager/adeCampus/table";
	}
	
	@RequestMapping(value = "/manager/adeCampus/params", produces = "text/html")
    public String displayParams(@PathVariable String emargementContext, Model uiModel) throws IOException, ParserConfigurationException, SAXException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String sessionId = adeService.getSessionId(false, emargementContext);
		uiModel.addAttribute("mapComposantes", adeService.getMapComposantes(sessionId));
		uiModel.addAttribute("mapSalles", adeService.getClassroomsList(sessionId));
		uiModel.addAttribute("mapProjets", adeService.getProjectLists(sessionId));
		uiModel.addAttribute("valuesComposantes", getValuesPref(auth.getName(), ADE_STORED_COMPOSANTE));
		uiModel.addAttribute("valuesSalles", getValuesPref(auth.getName(), ADE_STORED_SALLE));
		uiModel.addAttribute("valueProject", (!getValuesPref(auth.getName(), ADE_STORED_PROJET).isEmpty())? getValuesPref(auth.getName(), ADE_STORED_PROJET).get(0) :"");
		uiModel.addAttribute("rootCategories", getRootCategories());
		uiModel.addAttribute("categories",appliConfigService.getCategoriesAde());
		return "manager/adeCampus/params";
	}
	
	@RequestMapping(value = "/manager/adeCampus/salles", produces = "text/html")
    public String displaySalles(@PathVariable String emargementContext, Model uiModel, @RequestParam(value="codeSalle", required = false) String codeSalle) 
    		throws IOException, ParserConfigurationException, SAXException, ParseException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String sessionId = adeService.getSessionId(false, emargementContext);
		uiModel.addAttribute("valuesSalles", getValuesPref(auth.getName(), ADE_STORED_SALLE));
		uiModel.addAttribute("listeSalles", adeService.getListClassrooms(sessionId, codeSalle, null, null));
		uiModel.addAttribute("valueProject", (!getValuesPref(auth.getName(), ADE_STORED_PROJET).isEmpty())? getValuesPref(auth.getName(), ADE_STORED_PROJET).get(0) :"");
		uiModel.addAttribute("codeSalle", codeSalle);
		uiModel.addAttribute("campuses", campusRepository.findAll());		
		return "manager/adeCampus/salles";
	}
	
	@Transactional
	@PostMapping(value = "/manager/adeCampus/importEvents")
	@ResponseBody
	public String importEvent(@PathVariable String emargementContext, @RequestParam(value="btSelectItem", required = false) List<Long> idEvents, 
			@RequestParam(value="campuses", required = false) List<String> campuses,
			@RequestParam(value="codeComposante") String codeComposante, @RequestParam(value="strDateMin", required = false) String strDateMin,
			@RequestParam(value="existingSe", required = false) String existingSe,
			@RequestParam(value="idList", required = false) List<String> idList,
			@RequestParam(value="strDateMax", required = false) String strDateMax) throws IOException, ParserConfigurationException, SAXException, ParseException {
		if(idEvents!=null) {
			String sessionId = adeService.getSessionId(false, emargementContext);
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String idProject = prefsRepository.findByUserAppEppnAndNom(auth.getName(), ADE_STORED_PROJET).get(0).getValue();
			if(adeService.getConnectionProject(idProject, sessionId)==null) {
				sessionId = adeService.getSessionId(true, emargementContext);
				adeService.getConnectionProject(idProject, sessionId);
				log.info("Récupération du projet Ade " + idProject);
			}
			List<AdeResourceBean> beans = adeService.getAdeBeans(sessionId, strDateMin, strDateMax, idEvents, existingSe, codeComposante, idList);
			
			if(!beans.isEmpty()) {
				adeService.saveEvents(beans, sessionId, emargementContext, campuses, auth.getName(), false, null);
			}else {
				log.info("Aucun évènement à importer");
			}
		}
		
		return String.format("strDateMin=%s&strDateMax=%s&existingSe=true&codeComposante=%s&idList=%s", 
			    			emargementContext, strDateMin, strDateMax, codeComposante,StringUtils.join(idList, ","));
	}
	
	@PostMapping(value = "/manager/adeCampus/savePref")
	public String savePref(@PathVariable String emargementContext,String sessionId, @RequestParam("type") String type, 
			@RequestParam(value="btSelectItem", required = false) List<String> codeAde) throws IOException, ParserConfigurationException, SAXException  {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		preferencesService.updatePrefs(type, StringUtils.join(codeAde, ";;"), eppn, emargementContext);
		if(ADE_STORED_PROJET.equals(type)){
			if(sessionId == null) {
				sessionId= adeService.getSessionId(true, emargementContext);
			}
			adeService.getConnectionProject(codeAde.get(0), sessionId);
		}
		return String.format("redirect:/%s/manager/adeCampus/params", emargementContext);
	}
	
	@PostMapping(value = "/manager/adeCampus/saveConfig")
	public String saveconfig(@PathVariable String emargementContext, @RequestParam("catEtu") String catEtu, 
			@RequestParam("catForm") String catForm)  {
		List<AppliConfig> configs = appliConfigRepository.findAppliConfigByKey("ADE_CATEGORIES");
		AppliConfig adeConfig = configs.get(0);
		adeConfig.setValue(catEtu.concat(",").concat(catForm));
		appliConfigRepository.save(adeConfig);
		return String.format("redirect:/%s/manager/adeCampus/params", emargementContext);
	}
	
	public List<String> getValuesPref(String eppn, String pref) {
		List<String> values = new ArrayList<>(); 
		List<Prefs> prefsAdeStored = prefsRepository.findByUserAppEppnAndNom(eppn, pref);
		if(!prefsAdeStored.isEmpty()) {
			List<String> temp = prefsAdeStored.stream()
		              .map(Prefs::getValue)
		              .collect(Collectors.toList());
			if(temp.get(0) != null) {
				String [] splitAll = temp.get(0).split(";;");
				values = Arrays.asList(splitAll);
				Collections.sort(values);
			}else {
				prefsRepository.delete(prefsAdeStored.get(0));
			}
		}
		return values;
	}
	
	@Transactional
	@PostMapping(value = "/manager/adeCampus/importClassrooms")
	public String importClassrooms(@PathVariable String emargementContext, 
			@RequestParam(value="btSelectItem", required = false) List<Long> idClassrooms, String codeSalle, Campus campus) throws IOException, ParserConfigurationException, SAXException, ParseException {
		String sessionId = adeService.getSessionId(false, emargementContext);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<AdeClassroomBean> adeClassroomBeans = adeService.getListClassrooms(sessionId, codeSalle, null, idClassrooms);
		if(!adeClassroomBeans.isEmpty()) {
			Context ctx = contextRepository.findByContextKey(emargementContext);
			for(AdeClassroomBean bean : adeClassroomBeans) {
				Location location = null;
				Long adeClassRoomId = bean.getIdClassRoom();
				if(!locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx).isEmpty()){
					location = locationRepository.findByAdeClassRoomIdAndContext(adeClassRoomId, ctx).get(0);
				}else {
					location = new Location();
					location.setAdeClassRoomId(adeClassRoomId);
					location.setAdresse(bean.getChemin());
					location.setCampus(campus);
					location.setCapacite(bean.getSize());
					location.setContext(ctx);
					location.setNom(bean.getNom());
					locationRepository.save(location);
				}
			}
		}
		logService.log(ACTION.ADE_IMPORT, RETCODE.SUCCESS, "Import salles : " + adeClassroomBeans.size(), auth.getName(), null, emargementContext, auth.getName());
		return String.format("redirect:/%s/manager/adeCampus/salles?codeSalle=%s", emargementContext, codeSalle);
	}
	
	public List<String> getRootCategories(){
		return Arrays.asList(catArray);
	}
	
	@GetMapping(value = "/manager/adeCampus/disconnect")
	public String disconnect(@PathVariable String emargementContext) {
		adeService.disconnectSession();
		return String.format("redirect:/%s/manager/adeCampus", emargementContext);
	}
	
	@RequestMapping(value="/manager/adeCampus/json", headers = "Accept=application/json; charset=utf-8")
	@ResponseBody 
	public String getJsonAde(@PathVariable String emargementContext, @RequestParam(value="composante", required = false) String composante) {
    	return adeService.getJsonfile(composante, emargementContext);
	}
}
