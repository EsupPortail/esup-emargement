package org.esupportail.emargement.web.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Event;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.PropertiesForm;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EventRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.repositories.custom.SessionEpreuveRepositoryCustom;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.EventService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.PreferencesService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.SessionLocationService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import flexjson.JSONSerializer;
import net.fortuna.ical4j.data.ParserException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class SessionEpreuveController {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired	
	StoredFileRepository storedFileRepository;
	
	@Autowired
	SessionEpreuveRepositoryCustom sessionEpreuveRepositoryCustom;
	
	@Autowired
	CampusRepository campusRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired	
	TypeSessionRepository typeSessionRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired	
	ContextRepository contextRepository;
	
    @Resource
    SessionEpreuveService sessionEpreuveService;
    
    @Resource 
    PreferencesService preferencesService;
    
    @Resource
    SessionLocationService sessionLocationService;
    
    @Resource
    TagCheckService tagCheckService;
    
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	EventService eventService;
	
	@Resource
	HelpService helpService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
	EventRepository eventRepository;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	private final static String ITEM = "sessionEpreuve";
	
	private final static String SESSIONS_SORTBYSTATUT = "sessionsSortByStatut";
	private final static String SESSIONS_SORTBYTYPE = "sessionsSortByType";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/manager/sessionEpreuve")
	public String list(@PathVariable String emargementContext, Model model, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable, 
			@RequestParam(value="seNom", required = false) String seNom, @RequestParam(value="multiSearch", required = false) String multiSearch,
			SessionEpreuve sessionSearch) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Context ctx = contextRepository.findByKey(emargementContext);
		ExampleMatcher matcher = ExampleMatcher.matching()
		.withIgnorePaths("maxBadgeageAlert", "isProcurationEnabled", "isSessionLibre", "isSaveInExcluded", "isGroupeDisplayed")
		.withIgnoreNullValues()
		.withMatcher("statut", ExampleMatcher.GenericPropertyMatchers.exact())
		.withMatcher("anneeUniv", ExampleMatcher.GenericPropertyMatchers.exact());
		
		List<Prefs> prefsStatut = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYSTATUT);
		List<Prefs> prefsType = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYTYPE);
		
		if(multiSearch == null) {
			sessionSearch.setAnneeUniv(String.valueOf(sessionEpreuveService.getCurrentanneUniv()));
			String selectedStatut = (prefsStatut.isEmpty())? "" : prefsStatut.get(0).getValue();
			sessionSearch.setStatut((selectedStatut.isEmpty())? null : Statut.valueOf(selectedStatut));
			String selectedType = (prefsType.isEmpty())? "" : prefsType.get(0).getValue();
			sessionSearch.setTypeSession((selectedType.isEmpty())? null : typeSessionRepository.findById(Long.valueOf(selectedType)).get());
		}else{
			if(sessionSearch.getId()==null) {
				String prefStatutValue = (sessionSearch.getStatut() == null)? "" : sessionSearch.getStatut().name();
				preferencesService.updatePrefs(SESSIONS_SORTBYSTATUT, prefStatutValue, auth.getName(), emargementContext) ;
				String prefTypeValue = (sessionSearch.getTypeSession() == null)? "" : sessionSearch.getTypeSession().getId().toString();
				preferencesService.updatePrefs(SESSIONS_SORTBYTYPE, prefTypeValue, auth.getName(), emargementContext) ;	
			}
		}
		Example<SessionEpreuve> sessionQuery = Example.of(sessionSearch, matcher);
		Sort sort = pageable.getSort();
		if(sort.equals(Sort.unsorted())) {
			sort = Sort.by(Sort.Order.desc("dateExamen"),Sort.Order.asc("heureEpreuve"), Sort.Order.asc("finEpreuve"));
		}
		
		Page<SessionEpreuve> sessionEpreuvePage = null;
		
		if(sessionSearch.getId()!=null) {
			SessionEpreuve se = sessionEpreuveRepository.findById(sessionSearch.getId()).get();
			List<SessionEpreuve> ses = new ArrayList<SessionEpreuve>();
			ses.add(se);
			sessionEpreuvePage = new PageImpl<>(ses);
			sessionSearch.setStatut(se.getStatut());
			sessionSearch.setTypeSession(se.getTypeSession());
			sessionSearch.setAnneeUniv(se.getAnneeUniv());
			sessionSearch.setId(null);			
		}else {
			PageRequest newPageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
			sessionEpreuvePage = sessionEpreuveRepository.findAll(sessionQuery, newPageRequest);
		}
		
        sessionEpreuveService.computeCounters(sessionEpreuvePage.getContent());
        model.addAttribute("ctxId", contexteService.getcurrentContext().getId());
        model.addAttribute("years", sessionEpreuveService.getYears(emargementContext));
        model.addAttribute("sessionEpreuvePage", sessionEpreuvePage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("sessionSearch", sessionSearch);
        model.addAttribute("statuts", Statut.values());
        model.addAttribute("typesSession", sessionEpreuveService.getTypesSession(ctx.getId()));
		return "manager/sessionEpreuve/list";
	}
	
	@GetMapping(value = "/manager/sessionEpreuve/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		List<SessionEpreuve> list = new ArrayList<SessionEpreuve>();
		list.add(se);
		sessionEpreuveService.computeCounters(list);
        uiModel.addAttribute("sessionEpreuve", list.get(0));
        uiModel.addAttribute("attachments", storedFileRepository.findBySessionEpreuve(list.get(0)));
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "manager/sessionEpreuve/show";
    }
	
	@GetMapping(value = "/manager/sessionEpreuve/repartition/{id}", produces = "text/html")
    public String repartitionList(@PathVariable("id") Long id, Model uiModel) {
		uiModel.addAttribute("countTagChecksTiersTemps", tagCheckService.countNbTagCheckRepartitionNull(id, true));
		uiModel.addAttribute("countTagChecksNotTiersTemps", tagCheckService.countNbTagCheckRepartitionNull(id, false));
		uiModel.addAttribute("countTagChecksRepartisTiersTemps", tagCheckService.countNbTagCheckRepartitionNotNull(id, true));
		uiModel.addAttribute("countTagChecksRepartisNotTiersTemps", tagCheckService.countNbTagCheckRepartitionNotNull(id, false));
		uiModel.addAttribute("capaciteTotaleTiersTemps", sessionEpreuveService.countCapaciteTotalSessionLocations(id, true));
		uiModel.addAttribute("capaciteTotaleNotTiersTemps", sessionEpreuveService.countCapaciteTotalSessionLocations(id, false));
		uiModel.addAttribute("sessionEpreuve", sessionEpreuveRepository.findById(id).get());
		uiModel.addAttribute("help", helpService.getValueOfKey("repartition"));
		
		PropertiesForm form = new PropertiesForm();
		form.setList(sessionLocationService.getRepartition(id));
		uiModel.addAttribute("form", form);
        return "manager/sessionEpreuve/repartition";
    }
	
	@Transactional
	@GetMapping(value = "/manager/sessionEpreuve/executeRepartition/{id}", produces = "text/html")
    public String executeRepartition(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, 
    		@RequestParam(value="alphaOrder", required = false) Boolean alphaOrder, final RedirectAttributes redirectAttributes) {
		
		boolean isOver = sessionEpreuveService.executeRepartition(id, alphaOrder);
		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(id).get();
		if(Statut.CLOSED.equals(sessionEpreuve.getStatut())){
			isOver = true;
			log.info("Pas de répartition possible , la session " + sessionEpreuve.getNomSessionEpreuve() + " est cloturée");
		}
		if(! isOver) {
			log.info("Répartition effectuée pour la session : " + sessionEpreuve.getNomSessionEpreuve());
			logService.log(ACTION.EXECUTE_REPARTITION, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), null, null, emargementContext, null);
		}else {
			log.info("Pas de répartition possible pour la session : " + sessionEpreuve.getNomSessionEpreuve() + " , la capcité totale est inférieure au nombre de participants");
			logService.log(ACTION.EXECUTE_REPARTITION, RETCODE.FAILED, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), null, null, emargementContext, null);
		}
		redirectAttributes.addFlashAttribute("isOver",  isOver);
        return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
	
	@GetMapping(value = "/manager/sessionEpreuve/tagCheckList/{id}", produces = "text/html")
    public String getTagCheckList(@PathVariable("id") Long id, Model uiModel, @PageableDefault(size = 1, direction = Direction.ASC, sort = "person")  Pageable pageable) {
		
		Long count = tagCheckRepository.countBySessionLocationExpectedId(id);
				
		int size = pageable.getPageSize();
		if( size == 1 && count>0) {
			size = count.intValue();
		}
		
		Page<TagCheck> tagChecks =  tagCheckService.getListTagChecksBySessionLocationId(id, toolUtil.updatePageable(pageable, size), null, false);
				
		uiModel.addAttribute("sessionLocation", sessionLocationRepository.findById(id).get());
		uiModel.addAttribute("tagChecks", tagChecks);
		uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
		uiModel.addAttribute("selectAll", tagChecks.getTotalElements());
        return "manager/sessionEpreuve/tagCheckList";
    }
	
	@Transactional
	@GetMapping(value = "/manager/sessionEpreuve/deleteRepartition/{id}", produces = "text/html")
    public String deleteRepartition(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
		tagCheckService.resetSessionLocationExpected(id);
		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(id).get();
		log.info("Réinitialisation de la répartition possible pour la session : " + sessionEpreuve.getNomSessionEpreuve());
		logService.log(ACTION.RESET_REPARTITION, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), null, null, emargementContext, null);
        return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
	
    @GetMapping(value = "/manager/sessionEpreuve", params = "form", produces = "text/html")
    public String createForm(@PathVariable String emargementContext, Model uiModel, @RequestParam(value = "anneeUniv", required = false) String anneeUniv) throws IOException, ParserException, ParseException {
    	SessionEpreuve SessionEpreuve = new SessionEpreuve();
    	SessionEpreuve.setTypeBadgeage(TypeBadgeage.SALLE);
    	populateEditForm(uiModel, SessionEpreuve, anneeUniv, emargementContext);
    	List<Event> icsList = eventRepository.findByIsEnabledTrue();
    	eventService.setNbEvent(icsList);
    	uiModel.addAttribute("currentAnneeUniv", anneeUniv);
    	uiModel.addAttribute("icsList", icsList);
    	uiModel.addAttribute("events", eventService.getEventsListFromIcs(emargementContext, eventService.getAllUrlList() ));
        return "manager/sessionEpreuve/create";
    }
    
    @GetMapping(value = "/manager/sessionEpreuve/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	populateEditForm(uiModel, sessionEpreuve, null, emargementContext);
    	boolean isSessionLibreDisabled = true;
    	if(sessionEpreuve.getIsSessionLibre() || tagCheckRepository.countTagCheckBySessionEpreuveId(id) == 0) {
    		isSessionLibreDisabled = false;
    	}
    	uiModel.addAttribute("isSessionLibreDisabled", isSessionLibreDisabled);
    	uiModel.addAttribute("seId", id);
    	uiModel.addAttribute("strDateExamen", sessionEpreuve.getDateExamen());
    	uiModel.addAttribute("strDateFin", sessionEpreuve.getDateFin());
        return "manager/sessionEpreuve/update";
    }
    
    void populateEditForm(Model uiModel, SessionEpreuve sessionEpreuve, String anneeUniv, String emargementContext) {
    	uiModel.addAttribute("types", typeSessionRepository.findAllByOrderByLibelle());
    	uiModel.addAttribute("allCampuses", campusRepository.findAll());
    	uiModel.addAttribute("allGroupes", groupeRepository.findAll());
        uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
        uiModel.addAttribute("years", sessionEpreuveService.getYears(emargementContext));
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        uiModel.addAttribute("anneeUniv", anneeUniv);
        Date date = Calendar.getInstance().getTime();  
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
        String strDate = dateFormat.format(date);  
        uiModel.addAttribute("minDate", strDate);
    }
    
    @PostMapping("/manager/sessionEpreuve/create")
    public String create(@PathVariable String emargementContext, @Valid SessionEpreuve sessionEpreuve, BindingResult bindingResult, Model uiModel, 
    						HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes, @RequestParam("strDateExamen") String strDateExamen, 
    						@RequestParam("strDateFin") String strDateFin) throws IOException, ParseException {
    	
    	Date dateExamen=new SimpleDateFormat("yyyy-MM-dd").parse(strDateExamen);
    	sessionEpreuve.setDateExamen(dateExamen);
    	int compareEpreuve = toolUtil.compareDate(sessionEpreuve.getFinEpreuve(), sessionEpreuve.getHeureEpreuve(), "HH:mm");
    	int compareConvoc = toolUtil.compareDate(sessionEpreuve.getHeureEpreuve(), sessionEpreuve.getHeureConvocation(), "HH:mm");
    	int compareDebutFin = 0;
    	if(!strDateFin.isEmpty()) {
    		Date dateFin =new SimpleDateFormat("yyyy-MM-dd").parse(strDateFin);
    		sessionEpreuve.setDateFin(dateFin);
    		compareDebutFin = toolUtil.compareDate(sessionEpreuve.getDateExamen(), sessionEpreuve.getDateFin(), "yyyy-MM-dd");
    	}
    	//Pour éviter toute confusion lors du badgeage dans les requêtes de badgeage, le nom d'une session doit être unique me hors contexte !
    	Long count = sessionEpreuveRepository.countExistingNomSessionEpreuve(sessionEpreuve.getNomSessionEpreuve());
    	
    	if (bindingResult.hasErrors() || compareEpreuve<= 0 || compareConvoc<=0 ||	count > 0 || compareDebutFin > 0) {
            populateEditForm(uiModel, sessionEpreuve, null, emargementContext);
            uiModel.addAttribute("compareEpreuve", (compareEpreuve<= 0) ? true : false);
            uiModel.addAttribute("compareConvoc", (compareConvoc<= 0) ? true : false);
            uiModel.addAttribute("countExisting", sessionEpreuve.getNomSessionEpreuve());
            return "manager/sessionEpreuve/create";
        }
        uiModel.asMap().clear();
        if(sessionEpreuveRepository.countByNomSessionEpreuve(sessionEpreuve.getNomSessionEpreuve())>0) {
        	redirectAttributes.addFlashAttribute("nom", sessionEpreuve.getNomSessionEpreuve());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, session  déjà existante : " + sessionEpreuve.getNomSessionEpreuve());
        	return String.format("redirect:/%s/manager/sessionEpreuve?form", emargementContext);
        }else {
        	sessionEpreuve.setContext(contexteService.getcurrentContext());
        	sessionEpreuveService.save(sessionEpreuve, emargementContext);
        	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        	log.info("Création d'une session : " + sessionEpreuve.getNomSessionEpreuve());
        	logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), auth.getName(), null, emargementContext, null);
            return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
        }        
    }
    
    @PostMapping("/manager/sessionEpreuve/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid SessionEpreuve sessionEpreuve, 
    		@RequestParam("strDateExamen") String strDateExamen, @RequestParam("strDateFin") String strDateFin, BindingResult bindingResult, Model uiModel, 
    					HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) throws IOException, ParseException {
        
    	Date dateExamen=new SimpleDateFormat("yyyy-MM-dd").parse(strDateExamen);
    	sessionEpreuve.setDateExamen(dateExamen);
    	int compareEpreuve = toolUtil.compareDate(sessionEpreuve.getFinEpreuve(), sessionEpreuve.getHeureEpreuve(), "HH:mm");
    	int compareConvoc = toolUtil.compareDate(sessionEpreuve.getHeureEpreuve(), sessionEpreuve.getHeureConvocation(), "HH:mm");
    	int compareDebutFin = 0;
    	if(!strDateFin.isEmpty()) {
    		Date dateFin =new SimpleDateFormat("yyyy-MM-dd").parse(strDateFin);
    		sessionEpreuve.setDateFin(dateFin);
    		compareDebutFin = toolUtil.compareDate(sessionEpreuve.getDateExamen(), sessionEpreuve.getDateFin(), "yyyy-MM-dd");
    	}
    	Long count = sessionEpreuveRepository.countExistingNomSessionEpreuve(sessionEpreuve.getNomSessionEpreuve());
    	SessionEpreuve originalSe = sessionEpreuveRepository.findById(id).get();
    	if (bindingResult.hasErrors() || compareEpreuve<= 0 || compareConvoc<=0 || 
    			(count > 0 && !originalSe.getNomSessionEpreuve().equals(sessionEpreuve.getNomSessionEpreuve())) || compareDebutFin > 0) {
            populateEditForm(uiModel, sessionEpreuve, null, emargementContext);
            uiModel.addAttribute("compareEpreuve", (compareEpreuve<= 0) ? true : false);
            uiModel.addAttribute("compareConvoc", (compareConvoc<= 0) ? true : false);
            uiModel.addAttribute("countExisting", sessionEpreuve.getNomSessionEpreuve());
            return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
        }
        uiModel.asMap().clear();
        if(sessionEpreuveRepository.countByNomSessionEpreuve(sessionEpreuve.getNomSessionEpreuve())>0 && 
        			!sessionEpreuve.getNomSessionEpreuve().equalsIgnoreCase(sessionEpreuveRepository.findById(sessionEpreuve.getId()).get().getNomSessionEpreuve())) {
        	redirectAttributes.addFlashAttribute("nom", sessionEpreuve.getNomSessionEpreuve());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj, session  déjà existante : " + sessionEpreuve.getNomSessionEpreuve());
        	return String.format("redirect:/%s/manager/sessionEpreuve/%s?form", emargementContext, sessionEpreuve.getId());
        }else {
        	sessionEpreuve.setContext(contexteService.getcurrentContext());
        	sessionEpreuveService.save(sessionEpreuve, emargementContext);
        	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        	log.info("Maj d'une session : " + sessionEpreuve.getNomSessionEpreuve());
        	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), auth.getName(), null, emargementContext, null);
        	return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
        }        
    }
    
    @PostMapping(value = "/manager/sessionEpreuve/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	String nom = sessionEpreuve.getNomSessionEpreuve();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
			sessionEpreuveService.delete(sessionEpreuve);
			log.info("Suppression d'une session : " + nom);
			logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + nom, auth.getName(), null, emargementContext, null);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "constrainttError");
			log.info("Impossible de supprimer la session : " + nom, e);
			logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.FAILED, "Nom : " + nom, auth.getName(), null, emargementContext, null);
		}
        return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
    
    @PostMapping("/manager/sessionEpreuve/emargement")
    public void exportEamrgement(@PathVariable String emargementContext, @RequestParam("sessionLocationId") Long sessionLocationId, 
    			@RequestParam("sessionEpreuveId") Long sessionEpreuveId, @RequestParam("type") String type, HttpServletResponse response){
    	
    	sessionEpreuveService.exportEmargement(response, sessionLocationId, sessionEpreuveId, type, emargementContext);
    }
    
    @GetMapping("/manager/sessionEpreuve/search")
    @ResponseBody
    public List<SessionEpreuve> search(@RequestParam("searchValue") String searchString) throws ParseException {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<SessionEpreuve> sessionEpreuves= sessionEpreuveRepositoryCustom.findAll(searchString);
    	
        return sessionEpreuves;
    }
    
    
    @PostMapping("/manager/sessionEpreuve/affinerRepartition/{id}")
    @Transactional
    public String affinerRepartition(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel,@ModelAttribute(value="form") PropertiesForm  propertiesForm,
    		@RequestParam(value="alphaOrder", required = false) Boolean alphaOrder){
		 sessionEpreuveService.affinageRepartition(propertiesForm, emargementContext, alphaOrder);
    	 return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
    
    @Transactional
    @GetMapping("/manager/sessionEpreuve/duplicate/{id}")
    public String duplicateSession(@PathVariable String emargementContext, @PathVariable("id") Long id, final RedirectAttributes redirectAttributes) throws IOException {
    	
    	SessionEpreuve newSe = sessionEpreuveService.duplicateSessionEpreuve(id);
    	redirectAttributes.addFlashAttribute("duplicate", "duplicate");
    	return String.format("redirect:/%s/manager/sessionEpreuve/%s?form", emargementContext, newSe.getId());
    	
    }
    
    @PostMapping("/manager/sessionEpreuve/changeStatut/{id}")
    public String chngeStatut(@PathVariable String emargementContext, @PathVariable("id") Long id, 
    		@RequestParam("statut") Statut statut, final RedirectAttributes redirectAttributes) throws IOException {
    	
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	sessionEpreuve.setStatut(statut);
    	sessionEpreuveRepository.save(sessionEpreuve);
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	
    	redirectAttributes.addFlashAttribute("currentAnneeUniv", sessionEpreuve.getAnneeUniv());
    	log.info("Maj d'une session : " + sessionEpreuve.getNomSessionEpreuve());
    	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve() + " : " + "changement statut " + statut.name(), auth.getName(), null, emargementContext, null);
    	return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
    
    @GetMapping("/manager/sessionEpreuve/storedFiles/{id}")
    @ResponseBody
    public List<StoredFile> getStoredfiles(@PathVariable String emargementContext, @PathVariable("id") Long id){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		List<StoredFile> storedFiles = storedFileRepository.findBySessionEpreuve(se);
    	
        return storedFiles;
    }
    
    @Transactional
    @PostMapping("/manager/sessionEpreuve/storedFiles/delete")
    @ResponseBody
    public String  deleteStoredfile(@PathVariable String emargementContext, @RequestParam("key") Long key){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		StoredFile storedFile = storedFileRepository.findById(key).get();
		storedFileRepository.delete(storedFile);
		JSONSerializer serializer = new JSONSerializer();
		String flexJsonString = serializer.deepSerialize("zezez");
		return flexJsonString;
    }
    
	
	@Transactional
	@RequestMapping(value = "/manager/sessionEpreuve/{id}/photo")
	public void getPhoto(@PathVariable String emargementContext, @PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
		StoredFile sf = storedFileRepository.findById(id).get();
		if(sf != null) {
			Long size = sf.getFileSize();
			String contentType = sf.getContentType();
			response.setContentType(contentType);
			response.setContentLength(size.intValue());
			InputStream targetStream = new ByteArrayInputStream(sf.getBigFile().getBinaryFile());
			IOUtils.copy(targetStream, response.getOutputStream());
			///regarder les droits
		}
	}
	
	
}
