package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AdeClassroomBean;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Task;
import org.esupportail.emargement.domain.Task.Status;
import org.esupportail.emargement.exceptions.AdeApiRequestException;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.TaskRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.AdeService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.GroupeService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PreferencesService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.TaskService;
import org.esupportail.emargement.utils.ToolUtil;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.xml.sax.SAXException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class AdeController {
	
	private final static String ADE_STORED_SALLE = "adeStoredSalle";
	
	private final static String ADE_STORED_FORMATION = "adeStoredFormation";
	
	public final static String ADE_PLANIFICATION = "adePlanification";
	
	private final static String ITEM = "adeCampus";
	
	@Resource
	HelpService helpService;
	
	@Resource
	TaskService taskService;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${emargement.ade.home.url}")
	private String urlHomeAde;
	
	@Value("${emargement.ade.import.duree}")
	private String dureeMaxImport;
	
	@Value("${emargement.ade.import.cron}")
	private String cronAde;
    
	@ModelAttribute("active")
	public static String getActiveMenu() {
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
	
	@ModelAttribute("defaultCampus")
	public static String getDefaultCampus() {
		return ITEM;
	}
	
	@Resource
	AdeService adeService;
	
	@Resource
	LogService logService;
	
	@Resource
	GroupeService groupeService;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired	
	GroupeRepository groupeRepository;

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
	TagCheckRepository tagCheckRepository;
	
	@Autowired	
	TaskRepository taskRepository;
	
    @Resource 
    PreferencesService preferencesService;
    
    @Resource 
    SessionEpreuveService sessionEpreuveService;
    
    @Resource
    AppliConfigService appliConfigService;
    
    @Resource 
    LdapService ldapService;
    
	@Autowired
    ToolUtil toolUtil;
    
	@GetMapping(value = "/manager/adeCampus")
	public String index(@PathVariable String emargementContext, Model uiModel, @RequestParam(required = false) String projet){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			// Si projet est null alors on récupère l'id du projet en cours
			// sinon on utilise projet et on l'enregistre en tant que projet en cours
			String idProject = adeService.getCurrentProject(projet, auth.getName(), emargementContext) ;
			String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
			String fomrAde = appliConfigService.getFormationAde();
			String formationCat = (fomrAde!=null && !fomrAde.isEmpty())? fomrAde : null;
			uiModel.addAttribute("isAdeConfigOk", appliConfigService.getProjetAde().isEmpty()? false : true);
			uiModel.addAttribute("values", adeService.getPrefByContext(adeService.ADE_STORED_COMPOSANTE + idProject));
			uiModel.addAttribute("valuesFormation", adeService.getPrefByContext(ADE_STORED_FORMATION + idProject));
			uiModel.addAttribute("existingSe", true);
			uiModel.addAttribute("idProject", idProject);
			uiModel.addAttribute("projects", adeService.getProjectLists(sessionId));
			uiModel.addAttribute("category6", formationCat);
			uiModel.addAttribute("campuses", campusRepository.findAll());
			uiModel.addAttribute("isCreateGroupeAdeEnabled", appliConfigService.isAdeCampusGroupeAutoEnabled());
			uiModel.addAttribute("allGroupes", groupeRepository.findByAnneeUnivOrderByNom(String.valueOf(sessionEpreuveService.getCurrentanneUniv())));
		} catch (Exception e) {
			log.error("Erreur lors de la récupération des évènements", e);
		}
		return "manager/adeCampus/index";
	}
	
	@GetMapping(value = "/manager/adeCampus/Events")
	public String getTableEvents(@PathVariable String emargementContext, Model uiModel, 
			@RequestParam(required = false) String existingSe,
			@RequestParam(required = false) List<String> idList,
			@RequestParam(required = false) String codeComposante, 
			@RequestParam(required = false) String strDateMin,
			@RequestParam(required = false) String strDateMax){
	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try {
			String idProject = adeService.getCurrentProject(null, auth.getName(), emargementContext) ;
			String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
			uiModel.addAttribute("currentComposante", codeComposante);
			if("myEvents".equals(codeComposante) || idList.size()>0) {
				Context ctx = contextRepository.findByKey(emargementContext);
				uiModel.addAttribute("listEvents", adeService.getAdeBeans(sessionId, strDateMin, strDateMax, null, existingSe, codeComposante, idList, ctx, false));
			}
			uiModel.addAttribute("strDateMin", strDateMin);
			uiModel.addAttribute("strDateMax", strDateMax);
			uiModel.addAttribute("existingSe", (existingSe!=null)? true : false);
			uiModel.addAttribute("codeComposante", codeComposante);
			uiModel.addAttribute("campuses", campusRepository.findAll());
			uiModel.addAttribute("values", adeService.getPrefByContext(adeService.ADE_STORED_COMPOSANTE + idProject));
		} catch (Exception e) {
			log.error("Erreur lors de la récupération des évènements", e);
		}
		return "manager/adeCampus/table";
	}
	
	@RequestMapping(value = "/manager/adeCampus/params", produces = "text/html")
    public String displayParams(@PathVariable String emargementContext, Model uiModel, @RequestParam(required = false) String idProjet) throws AdeApiRequestException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		// Si idProjet est null alors on récupère l'id du projet en cours
		// sinon on utilise idProjet et on l'enregistre en tant que projet en cours
		String idProject = adeService.getCurrentProject(idProjet, auth.getName(), emargementContext);
		String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
		uiModel.addAttribute("mapComposantes", adeService.getMapComposantesFormations(sessionId, "trainee"));
		String formAde = appliConfigService.getFormationAde();
		Map<String, String> mapFormations = (formAde!=null && !formAde.isEmpty())? adeService.getMapComposantesFormations(sessionId, formAde) : null;
		String adeComposantes = adeService.ADE_STORED_COMPOSANTE + idProject;
		uiModel.addAttribute("mapFormations", mapFormations);
		uiModel.addAttribute("mapSalles", adeService.getClassroomsList(sessionId));
		uiModel.addAttribute("idProject", idProject);
		uiModel.addAttribute("isAdeConfigOk", appliConfigService.getProjetAde().isEmpty()? false : true);
		uiModel.addAttribute("projects", adeService.getProjectLists(sessionId));
		uiModel.addAttribute("prefComp", !prefsRepository.findByNom(adeComposantes).isEmpty()? prefsRepository.findByNom(adeComposantes).get(0) : null);
		uiModel.addAttribute("valuesComposantes", adeService.getPrefByContext(adeComposantes));
		uiModel.addAttribute("valuesSalles", adeService.getPrefByContext(ADE_STORED_SALLE + idProject));
		uiModel.addAttribute("valuesFormations", adeService.getPrefByContext(ADE_STORED_FORMATION + idProject));
		uiModel.addAttribute("valuePlanification", adeService.getPrefByContext(ADE_PLANIFICATION + idProject));
		uiModel.addAttribute("nomProjet", adeService.getProjectLists(sessionId).get(idProject));
		return "manager/adeCampus/params";
	}
	
	@GetMapping(value = "/manager/adeCampus/salles", produces = "text/html")
    public String displaySalles(@PathVariable String emargementContext, Model uiModel, @RequestParam(required = false) String codeSalle, 
    		@RequestParam(required = false) String idProjet) 
			throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		// Si idProjet est null alors on récupère l'id du projet en cours
		// sinon on utilise idProjet et on l'enregistre en tant que projet en cours
		String idProject = adeService.getCurrentProject(idProjet, auth.getName(), emargementContext);
		String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
		uiModel.addAttribute("isAdeConfigOk", appliConfigService.getProjetAde().isEmpty()? false : true);
		uiModel.addAttribute("valuesSalles", adeService.getPrefByContext(ADE_STORED_SALLE + idProject));
		uiModel.addAttribute("listeSalles", codeSalle!=null && !codeSalle.isEmpty()? adeService.getListClassrooms2(sessionId, codeSalle, null) :  new ArrayList());
		uiModel.addAttribute("idProject", idProject);
		uiModel.addAttribute("projects", adeService.getProjectLists(sessionId));
		uiModel.addAttribute("codeSalle", codeSalle);
		uiModel.addAttribute("campuses", campusRepository.findAll());		
		return "manager/adeCampus/salles";
	}
	
	@Transactional
	@PostMapping(value = "/manager/adeCampus/importEvents")
	public String importEvent(@PathVariable String emargementContext, @RequestParam(value="btSelectItem", required = false) List<Long> idEvents, 
			@RequestParam(required = false) Campus campus,
			@RequestParam String codeComposante,
			@RequestParam(required = false) String strDateMin,
			@RequestParam(required = false) String existingSe,
			@RequestParam(required = false) List<String> idList,
			@RequestParam(required = false) String strDateMax,
			@RequestParam(required = false) List<Long> existingGroupe,
			@RequestParam(required = false) String newGroupe,
			@RequestParam(required = false) String idProject) throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
			adeService.importEvents(idEvents, emargementContext, strDateMin, strDateMax,newGroupe, existingGroupe, existingSe, 
					codeComposante,	campus,  idList, null, idProject, null, false);
		
		return String.format("redirect:/%s/manager/adeCampus/Events?strDateMin=%s&strDateMax=%s&existingSe=true&codeComposante=%s&idList=%s", 
			    			emargementContext, strDateMin, strDateMax, codeComposante,StringUtils.join(idList, ","));
	}
	
	@Transactional
	@PostMapping(value = "/manager/adeCampus/task/importEvents")
	public String importEventFromTask(@PathVariable String emargementContext,@RequestParam Task task) throws AdeApiRequestException, IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		Long dureeMax =  (dureeMaxImport == null || dureeMaxImport.isEmpty())? null : Long.valueOf(dureeMaxImport);
		taskService.processTask(task, emargementContext, dureeMax, 1);
		return String.format("redirect:/%s/manager/adeCampus/tasks", emargementContext);
	}
	
	@PostMapping(value = "/manager/adeCampus/removePrefs")
	public String removePrefs(@PathVariable String emargementContext, @RequestParam String idProject){
		String adeComoosantes = adeService.ADE_STORED_COMPOSANTE + idProject;
		preferencesService.removePrefs(null, ADE_STORED_FORMATION + idProject);
		preferencesService.removePrefs(null, adeComoosantes);
		preferencesService.removePrefs(null, ADE_STORED_SALLE + idProject);
		preferencesService.removePrefs(null, ADE_PLANIFICATION + idProject);
		return String.format("redirect:/%s/manager/adeCampus/params?idProjet=%s", emargementContext, idProject);
	}

	@Transactional
	@PostMapping(value = "/manager/adeCampus/importClassrooms")
	public String importClassrooms(@PathVariable String emargementContext, 
			@RequestParam(value="btSelectItem", required = false) List<Long> idClassrooms, String codeSalle, Campus campus) throws AdeApiRequestException, ParseException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String projectId = adeService.getCurrentProject(null, auth.getName(), emargementContext);
		String sessionId = adeService.getSessionIdByProjectId(projectId, emargementContext);
		List<AdeClassroomBean> adeClassroomBeans = adeService.getListClassrooms(sessionId, null, idClassrooms);
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
	
	@GetMapping(value = "/manager/adeCampus/disconnect")
	public String disconnect(@PathVariable String emargementContext) {
		adeService.disconnectSession(emargementContext);
		return String.format("redirect:/%s/manager/adeCampus", emargementContext);
	}
	
	@RequestMapping(value="/manager/adeCampus/json", headers = "Accept=application/json; charset=utf-8")
	@ResponseBody 
	public String getJsonAde(@PathVariable String emargementContext, @RequestParam(required = false) String fatherId,
			@RequestParam(required = false) String category, @RequestParam String idProject) {
    	return adeService.getJsonfile(fatherId, emargementContext, category, idProject);
	}
	
	@PostMapping(value = "/manager/adeCampus/createTask")
	public String createTask(@PathVariable String emargementContext, @RequestParam(required = false) String params,
			@RequestParam String libelles, @RequestParam String codeComposante,
			@RequestParam Campus campus,
			@RequestParam String idProject,
			final RedirectAttributes redirectAttributes){
		if(params.length() == 0) {
			redirectAttributes.addFlashAttribute("message", "message");
			return String.format("redirect:/%s/manager/adeCampus", emargementContext);
		}

		String [] splitParams  = params.split(",");
		String [] splitLibelles  = libelles.split(",");
		for(int i=0; i<splitParams.length; i++) {
			String param = splitParams[i].trim();
			String libelle = splitLibelles[i].trim();
				if(taskRepository.findByContextKeyAndParam(emargementContext, param).isEmpty()){
					Task task = new Task();
					task.setContext(contextRepository.findByKey(emargementContext));
					task.setAdeProject(idProject);
					task.setParam(param);
					task.setLibelle(libelle);
					task.setStatus(Status.NOTASK);
					task.setNbModifs(0);
					task.setDateCreation(new Date());
					task.setCampus(campus);
					task.setIsActif(true);
					task.setComposante(codeComposante);
					taskRepository.save(task);
					log.info("Tâche créée pour ce diplôme : " + libelle);
					logService.log(ACTION.TASK_CREATE, RETCODE.SUCCESS, task.getLibelle(), null,
							null, emargementContext, null);
				}else {
					log.info("Une tâche pour ce diplôme [" + libelle + "] existe déjà.");
				}
		}
		return String.format("redirect:/%s/manager/adeCampus/tasks", emargementContext);
	}

	@GetMapping(value = "/manager/adeCampus/tasks")
	public String tasks(@PathVariable String emargementContext, Model uiModel, @RequestParam(required = false)  String idProjet,
			@RequestParam(required = false) Boolean isActif) {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
  			// Si idProjet est null alors on récupère l'id du projet en cours
			// sinon on utilise idProjet et on l'enregistre en tant que projet en cours
			String idProject = adeService.getCurrentProject(idProjet, auth.getName(), emargementContext);
			String sessionId = adeService.getSessionIdByProjectId(idProject, emargementContext);
			Integer dureeMax =  (dureeMaxImport == null || dureeMaxImport.isEmpty())? null : Integer.valueOf(dureeMaxImport);
			uiModel.addAttribute("tasksList", taskRepository.findByAdeProject(idProject));
			uiModel.addAttribute("dureeMaxImport", toolUtil.convertirSecondes(dureeMax));
			uiModel.addAttribute("cronExpression", toolUtil.getCronExpression(cronAde));
			uiModel.addAttribute("projects", adeService.getProjectLists(sessionId));
			uiModel.addAttribute("idProject", idProject);
			uiModel.addAttribute("isAdeConfigOk", appliConfigService.getProjetAde().isEmpty()? false : true);
			uiModel.addAttribute("isActif", isActif);
			uiModel.addAttribute("valuePlanification", adeService.getPrefByContext(ADE_PLANIFICATION + idProject));
		} catch (Exception e) {
			log.error("Erreur lors de connexion session/projet ADE", e);
		}
		return "manager/adeCampus/tasks";
	}
		
    @Transactional
    @PostMapping(value = "/manager/adeCampus/tasks/delete/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Task task) {
    	taskRepository.delete(task);
    	logService.log(ACTION.TASK_DELETE, RETCODE.SUCCESS, task.getLibelle(), null,
				null, emargementContext, null);
        return String.format("redirect:/%s/manager/adeCampus/tasks", emargementContext);
    }
	
    @Transactional
	@PostMapping(value = "/manager/adeCampus/saveParams")
	public String saveParams(@PathVariable String emargementContext, @RequestParam(required = false) String composantes, 
			@RequestParam(required = false) String formations, @RequestParam(required = false) String salles, @RequestParam(required = false) String planification,
			@RequestParam String idProject)  {
    	if(!idProject.isEmpty()) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String adeComposantes = adeService.ADE_STORED_COMPOSANTE + idProject;
			String adeFormations = ADE_STORED_FORMATION + idProject;
			String adeSalles = ADE_STORED_SALLE + idProject;
			String adePlanification = ADE_PLANIFICATION + idProject;
			String eppn = auth.getName();
			preferencesService.removePrefs(null, adeFormations);
			if(formations != null) {
				preferencesService.updatePrefs(adeFormations, formations, eppn, emargementContext, "dummy");
			}
			preferencesService.removePrefs(null, adeComposantes);
			if(composantes != null) {
				preferencesService.updatePrefs(adeComposantes, composantes, eppn, emargementContext, "dummy");
			}
			preferencesService.removePrefs(null, adeSalles);
			if(salles != null) {
				preferencesService.updatePrefs(adeSalles, salles, eppn, emargementContext, "dummy");
			}
			preferencesService.removePrefs(null, adePlanification);
			if(!planification.isEmpty()) {
				preferencesService.updatePrefs(adePlanification, planification, eppn, emargementContext, "dummy");
			}
    	}
		return String.format("redirect:/%s/manager/adeCampus/params?idProjet=%s", emargementContext, idProject);
	}
    
    @PostMapping("/manager/adeCampus/updatetask")
    public String updateTask(@PathVariable String emargementContext, @RequestParam(required = false) Task task, 
    		@RequestParam(required = false) boolean isActif, @RequestParam String idProjet, @RequestParam boolean isAll) {
    	if(isAll) {
    		log.info("isAll");
    		List<Task> tasks = taskRepository.findByAdeProject(idProjet);
    		if(!tasks.isEmpty()) {
    			for (Task t : tasks) {
    				t.setIsActif(isActif);
    		    	taskRepository.save(t);
    			}
    		}
    	}else {
	    	task.setIsActif(isActif);
	    	taskRepository.save(task);
    	}
    	logService.log(ACTION.TASK_UPDATE, RETCODE.SUCCESS, "Toutes : " + isAll, null,
				null, emargementContext, null);
        return String.format("redirect:/%s/manager/adeCampus/tasks?idProjet=%s", emargementContext, idProjet);
    }
}
