package org.esupportail.emargement.web.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.PropertiesForm;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeBadgeage;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StatutSessionRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.custom.SessionEpreuveRepositoryCustom;
import org.esupportail.emargement.services.AdeService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.xml.sax.SAXException;

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
	
	@Autowired
	StatutSessionRepository statutSessionRepository;
	
    @Resource
    SessionEpreuveService sessionEpreuveService;
    
    @Resource 
    PreferencesService preferencesService;
    
    @Resource
    SessionLocationService sessionLocationService;
    
    @Resource
    AppliConfigService appliConfigService;
    
    @Resource
    TagCheckService tagCheckService;
    
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	HelpService helpService;
	
	@Resource
	AdeService adeService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	private final static String ITEM = "sessionEpreuve";
	
	private final static String SESSIONS_SORTBYSTATUT = "sessionsSortByStatut";
	private final static String SESSIONS_SORTBYTYPE = "sessionsSortByType";
	private final static String SESSIONS_SORTBYPERIOD = "sessionsSortByPeriod";
	private final static String SESSIONS_SORTBYCAMPUS = "sessionsSortByCampus";
	private final static String SESSIONS_SORTBYLISTE = "sessionsSortByListe";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/manager/sessionEpreuve")
	public String list(@PathVariable String emargementContext, Model model, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable, 
			@RequestParam(required = false) String multiSearch, @RequestParam(required = false) Long searchString,
			SessionEpreuve sessionSearch,  @RequestParam(required = false) String dateSessions,
			@RequestParam(required = false) String view) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	UserApp userApp = userAppRepository.findByEppnAndContextKey(auth.getName(), emargementContext);
		Context ctx = contextRepository.findByKey(emargementContext);
		ExampleMatcher matcher = ExampleMatcher.matching()
		.withIgnorePaths("maxBadgeageAlert", "isProcurationEnabled", "isSessionLibre", "isSaveInExcluded", "isGroupeDisplayed", "isSecondTag")
		.withIgnoreNullValues()
		.withMatcher("statut", ExampleMatcher.GenericPropertyMatchers.exact())
		.withMatcher("typeSession", ExampleMatcher.GenericPropertyMatchers.exact())
		.withMatcher("anneeUniv", ExampleMatcher.GenericPropertyMatchers.exact())
		.withMatcher("campus", ExampleMatcher.GenericPropertyMatchers.exact());

		List<Prefs> prefsStatut = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYSTATUT);
		List<Prefs> prefsType = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYTYPE);
		List<Prefs> prefsPeriod = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYPERIOD);
		List<Prefs> prefsCampus = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYCAMPUS);
		List<Prefs> prefsListe = prefsRepository.findByUserAppEppnAndNom(auth.getName(), SESSIONS_SORTBYLISTE);
		if(searchString != null) {
			sessionSearch.setId(searchString);
		}
		if(dateSessions == null) {
			dateSessions = (prefsPeriod.isEmpty())? "all" : prefsPeriod.get(0).getValue();
		}
		if(multiSearch == null) {
			sessionSearch.setAnneeUniv(String.valueOf(sessionEpreuveService.getLastAnneeUniv(emargementContext)));
			String selectedStatut = (prefsStatut.isEmpty())? "" : prefsStatut.get(0).getValue();
			sessionSearch.setStatutSession((selectedStatut.isEmpty())? null : statutSessionRepository.findByKey(selectedStatut));
			String selectedType = (prefsType.isEmpty())? "" : prefsType.get(0).getValue();
			sessionSearch.setTypeSession((selectedType.isEmpty())? null : typeSessionRepository.findById(Long.valueOf(selectedType)).get());
			String selectedCampus= (prefsCampus.isEmpty())? "" : prefsCampus.get(0).getValue();
			sessionSearch.setCampus((selectedCampus.isEmpty())? null : campusRepository.findById(Long.valueOf(selectedCampus)).get());
			
			String selectedListe= (prefsListe.isEmpty())? "" : prefsListe.get(0).getValue();
			view = selectedListe.isEmpty()? "all" : selectedListe;
		}else{
			if(sessionSearch.getId()==null) {
				String prefStatutValue = (sessionSearch.getStatutSession() == null)? "" : sessionSearch.getStatutSession().getKey();
				preferencesService.updatePrefs(SESSIONS_SORTBYSTATUT, prefStatutValue, auth.getName(), emargementContext, "dummy") ;
				String prefTypeValue = (sessionSearch.getTypeSession() == null)? "" : sessionSearch.getTypeSession().getId().toString();
				preferencesService.updatePrefs(SESSIONS_SORTBYTYPE, prefTypeValue, auth.getName(), emargementContext, "dummy") ;
				String prefPeriodValue = (dateSessions == null)? "all" : dateSessions;
				preferencesService.updatePrefs(SESSIONS_SORTBYPERIOD, prefPeriodValue, auth.getName(), emargementContext, "dummy") ;
				String prefCampusValue = (sessionSearch.getCampus() == null)? "" : sessionSearch.getCampus().getId().toString();
				preferencesService.updatePrefs(SESSIONS_SORTBYCAMPUS, prefCampusValue, auth.getName(), emargementContext, "dummy") ;
				String prefsListeValue = (view == null)? "" : view;
				preferencesService.updatePrefs(SESSIONS_SORTBYLISTE, prefsListeValue, auth.getName(), emargementContext, "dummy") ;
			}
		}
		Example<SessionEpreuve> sessionQuery = Example.of(sessionSearch, matcher);
		Sort sort = pageable.getSort();
		if(sort.equals(Sort.unsorted())) {
			sort = Sort.by(Sort.Order.desc("dateExamen"),Sort.Order.asc("heureEpreuve"), Sort.Order.asc("finEpreuve"));
		}

		Page<SessionEpreuve> sessionEpreuvePage = null;
		
		if(sessionSearch.getId()!=null && sessionEpreuveRepository.findById(sessionSearch.getId()).isPresent() ) {
			SessionEpreuve se = sessionEpreuveRepository.findById(sessionSearch.getId()).get();
			List<SessionEpreuve> ses = new ArrayList<>();
			ses.add(se);
			sessionEpreuvePage = new PageImpl<>(ses);
			sessionSearch.setStatutSession(se.getStatutSession());
			sessionSearch.setTypeSession(se.getTypeSession());
			sessionSearch.setAnneeUniv(se.getAnneeUniv());
			sessionSearch.setId(null);			
		}else {
			PageRequest newPageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
			Date dateDebut = sessionEpreuveService.setDateSessionEpreuve("dateDebut", dateSessions);
			Date dateFin = sessionEpreuveService.setDateSessionEpreuve("dateFin", dateSessions);
			
			sessionEpreuvePage= sessionEpreuveRepository.findAll(
					sessionEpreuveRepositoryCustom.getSpecFromDatesAndExample(dateDebut, dateFin, sessionQuery, view, userApp), newPageRequest);
		}
        sessionEpreuveService.computeCounters(sessionEpreuvePage.getContent());
        model.addAttribute("ctxId", contexteService.getcurrentContext().getId());
        model.addAttribute("years", sessionEpreuveService.getYears(emargementContext));
        model.addAttribute("sessionEpreuvePage", sessionEpreuvePage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("sessionSearch", sessionSearch);
        model.addAttribute("statuts", statutSessionRepository.findAll());
        model.addAttribute("typesSession", sessionEpreuveService.getTypesSession(ctx.getId()));
        model.addAttribute("sites", campusRepository.findByOrderBySite());
        model.addAttribute("dateSessions", dateSessions);
        model.addAttribute("view", view);
        model.addAttribute("userApp", userApp);
		return "manager/sessionEpreuve/list";
	}

	@GetMapping(value = "/manager/sessionEpreuve/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel, @RequestParam(required = false) String modal) {
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		List<SessionEpreuve> list = new ArrayList<>();
		list.add(se);
		sessionEpreuveService.computeCounters(list);
        uiModel.addAttribute("sessionEpreuve", list.get(0));
        uiModel.addAttribute("attachments", storedFileRepository.findBySessionEpreuve(list.get(0)));
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        if(modal != null){
        	return "manager/sessionEpreuve/show :: modalContent";
        }
        return "manager/sessionEpreuve/show";
    }
	
	@GetMapping(value = "/manager/sessionEpreuve/repartition/{id}", produces = "text/html")
    public String repartitionList(@PathVariable Long id, Model uiModel, @ModelAttribute String tagCheckOrderValue) {
		Long totalArepartirTagChecks = tagCheckService.countNbTagCheckRepartitionNull(id, true) + tagCheckService.countNbTagCheckRepartitionNull(id, false);
		Long totalRepartisTagChecks = tagCheckService.countNbTagCheckRepartitionNotNull(id, true) + tagCheckService.countNbTagCheckRepartitionNotNull(id, false);
		uiModel.addAttribute("countTagChecksTiersTemps", tagCheckService.countNbTagCheckRepartitionNull(id, true));
		uiModel.addAttribute("countTagChecksNotTiersTemps", tagCheckService.countNbTagCheckRepartitionNull(id, false));
		uiModel.addAttribute("countTagChecksRepartisTiersTemps", tagCheckService.countNbTagCheckRepartitionNotNull(id, true));
		uiModel.addAttribute("countTagChecksRepartisNotTiersTemps", tagCheckService.countNbTagCheckRepartitionNotNull(id, false));
		uiModel.addAttribute("capaciteTotaleTiersTemps", sessionEpreuveService.countCapaciteTotalSessionLocations(id, true));
		uiModel.addAttribute("capaciteTotaleNotTiersTemps", sessionEpreuveService.countCapaciteTotalSessionLocations(id, false));
		uiModel.addAttribute("totalArepartirTagChecks", totalArepartirTagChecks);
		uiModel.addAttribute("totalRepartisTagChecks", totalRepartisTagChecks);
		uiModel.addAttribute("sessionEpreuve", sessionEpreuveRepository.findById(id).get());
		uiModel.addAttribute("help", helpService.getValueOfKey("repartition"));
		uiModel.addAttribute("tagCheckOrderValue", (tagCheckOrderValue.isEmpty())? null : tagCheckOrderValue);
		
		PropertiesForm form = new PropertiesForm();
		form.setList(sessionLocationService.getRepartition(id));
		uiModel.addAttribute("form", form);
        return "manager/sessionEpreuve/repartition";
    }
	
	@Transactional
	@GetMapping(value = "/manager/sessionEpreuve/executeRepartition/{id}", produces = "text/html")
    public String executeRepartition(@PathVariable String emargementContext, @PathVariable Long id, 
    		@RequestParam(value="alphaOrder", required = false) String tagCheckOrder, final RedirectAttributes redirectAttributes) {
		
		boolean isOver = sessionEpreuveService.executeRepartition(id, tagCheckOrder);
		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(id).get();
		if("CLOSED".equals(sessionEpreuve.getStatutSession().getKey())){
			isOver = true;
			log.info("Pas de répartition possible , la session " + sessionEpreuve.getNomSessionEpreuve() + " est cloturée");
		}
		if(! isOver) {
			log.info("Répartition effectuée pour la session : " + sessionEpreuve.getNomSessionEpreuve());
			logService.log(ACTION.EXECUTE_REPARTITION, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), null, null, emargementContext, null);
		}else {
			log.info("Pas de répartition possible pour la session : " + sessionEpreuve.getNomSessionEpreuve() + " , la capacité totale est inférieure au nombre de participants");
			logService.log(ACTION.EXECUTE_REPARTITION, RETCODE.FAILED, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), null, null, emargementContext, null);
		}
		redirectAttributes.addFlashAttribute("isOver",  isOver);
		redirectAttributes.addFlashAttribute("tagCheckOrderValue", tagCheckOrder);
        return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
	
	@GetMapping(value = "/manager/sessionEpreuve/tagCheckList/{id}", produces = "text/html")
    public String getTagCheckList(@PathVariable Long id, Model uiModel, @PageableDefault(size = 1, direction = Direction.ASC, sort = "person")  Pageable pageable) {
		
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
    public String deleteRepartition(@PathVariable String emargementContext, @PathVariable Long id) {
		tagCheckService.resetSessionLocationExpected(id);
		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(id).get();
		log.info("Réinitialisation de la répartition possible pour la session : " + sessionEpreuve.getNomSessionEpreuve());
		logService.log(ACTION.RESET_REPARTITION, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), null, null, emargementContext, null);
        return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
	
	@GetMapping(value = "/manager/sessionEpreuve", params = "form", produces = "text/html")
	public String createForm(@PathVariable String emargementContext, Model uiModel, 
	                         @RequestParam(required = false) String anneeUniv, 
	                         @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
	    SessionEpreuve sessionEpreuve = new SessionEpreuve();
	    sessionEpreuve.setTypeBadgeage(TypeBadgeage.SALLE);
	    sessionEpreuve.setCampus(
	    		campusRepository.findAll().stream()
	    	        .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
	    	        .findFirst()
	    	        .orElse(null)
	    	);
	    populateEditForm(uiModel, sessionEpreuve, anneeUniv, emargementContext);
	    uiModel.addAttribute("currentAnneeUniv", anneeUniv);
	    if (hxRequest != null) {
	    	return "manager/sessionEpreuve/create-modal :: modal-create";
	    }
	    return "manager/sessionEpreuve/create";
	}
    
    @GetMapping(value = "/manager/sessionEpreuve/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable String emargementContext, @PathVariable Long id, Model uiModel) {
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	populateEditForm(uiModel, sessionEpreuve, null, emargementContext);
    	boolean isSessionLibreDisabled = true;
    	if(sessionEpreuve.getIsSessionLibre() || tagCheckRepository.countTagCheckBySessionEpreuveId(id) == 0) {
    		isSessionLibreDisabled = false;
    	}
    	uiModel.addAttribute("isSessionLibreDisabled", isSessionLibreDisabled);
    	uiModel.addAttribute("seId", id);
    	uiModel.addAttribute("typePj", "session");
    	uiModel.addAttribute("strDateExamen", sessionEpreuve.getDateExamen());
    	uiModel.addAttribute("strDateFin", sessionEpreuve.getDateFin());
        return "manager/sessionEpreuve/update";
    }
    
    void populateEditForm(Model uiModel, SessionEpreuve sessionEpreuve, String anneeUniv, String emargementContext) {
    	uiModel.addAttribute("types", typeSessionRepository.findAllByOrderByLibelle());
    	uiModel.addAttribute("allCampuses", campusRepository.findAll());
    	uiModel.addAttribute("allGroupes", groupeRepository.findAll());
        uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        Date date = Calendar.getInstance().getTime();  
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
        String strDate = dateFormat.format(date);  
        uiModel.addAttribute("minDate", strDate);
    }
    
    @PostMapping("/manager/sessionEpreuve/create")
    public String create(@PathVariable String emargementContext, @Valid SessionEpreuve sessionEpreuve, BindingResult bindingResult, 
    		Model uiModel, final RedirectAttributes redirectAttributes, @RequestParam String strDateExamen, 
    		@RequestParam String strDateFin, @RequestHeader(value = "HX-Request", required = false) String hxRequest) throws IOException, ParseException {
    	Date dateExamen=new SimpleDateFormat("yyyy-MM-dd").parse(strDateExamen);
    	sessionEpreuve.setDateExamen(dateExamen);
    	if(sessionEpreuve.getHeureConvocation() == null) {
        	Calendar c = Calendar.getInstance();
    	    c.setTime(sessionEpreuve.getHeureEpreuve());
    	    c.add(Calendar.MINUTE, -15);
    	    sessionEpreuve.setHeureConvocation(c.getTime());
    	}
    	int compareEpreuve = toolUtil.compareDate(sessionEpreuve.getFinEpreuve(), sessionEpreuve.getHeureEpreuve(), "HH:mm");
    	int compareConvoc = toolUtil.compareDate(sessionEpreuve.getHeureEpreuve(), sessionEpreuve.getHeureConvocation(), "HH:mm");
    	int compareDebutFin = 0;
    	if(!strDateFin.isEmpty()) {
    		Date dateFin =new SimpleDateFormat("yyyy-MM-dd").parse(strDateFin);
    		sessionEpreuve.setDateFin(dateFin);
    		compareDebutFin = toolUtil.compareDate(sessionEpreuve.getDateExamen(), sessionEpreuve.getDateFin(), "yyyy-MM-dd");
    	}
    	
    	if (bindingResult.hasErrors() || compareEpreuve<= 0 || compareConvoc<=0 || compareDebutFin > 0) {
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
        }
		sessionEpreuve.setContext(contexteService.getcurrentContext());
		sessionEpreuve.setStatutSession(sessionEpreuveService.getStatutSession(sessionEpreuve));
		sessionEpreuve.setAnneeUniv(String.valueOf(sessionEpreuveService.getCurrentAnneeUnivFromDate(sessionEpreuve.getDateExamen())));
		sessionEpreuveService.save(sessionEpreuve, emargementContext, null);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		log.info("Création d'une session : " + sessionEpreuve.getNomSessionEpreuve());
		logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), auth.getName(), null, emargementContext, null);
		
		 if (hxRequest != null) {
			 uiModel.addAttribute("sessionEpreuve",sessionEpreuve);
			 return "manager/sessionEpreuve/modal-show1 ::show-modal";
		 }
		 return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
		 /*
		 return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s#openmodal-%d", 
				emargementContext, 
				sessionEpreuve.getAnneeUniv(),
				sessionEpreuve.getId() 
			);    */
    }
    
    @PostMapping("/manager/sessionEpreuve/update/{id}")
    public String update(@PathVariable String emargementContext, @Valid SessionEpreuve sessionEpreuve, 
    		@RequestParam String strDateExamen, @RequestParam String strDateFin, @RequestParam(required = false) String keyStatut, BindingResult bindingResult, 
    		Model uiModel) throws IOException, ParseException {
        
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
    	
    	if (bindingResult.hasErrors() || compareEpreuve<= 0 || compareConvoc<=0 || compareDebutFin > 0) {
            populateEditForm(uiModel, sessionEpreuve, null, emargementContext);
            uiModel.addAttribute("compareEpreuve", (compareEpreuve<= 0) ? true : false);
            uiModel.addAttribute("compareConvoc", (compareConvoc<= 0) ? true : false);
            return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
        }
        uiModel.asMap().clear();
    	sessionEpreuve.setContext(contexteService.getcurrentContext());
    	if(keyStatut!= null && !keyStatut.isEmpty()) {
    		if(keyStatut.equals("FORWARDED") ||  keyStatut.equals("PROCESSED") || keyStatut.equals("CANCELLED")) {
    			sessionEpreuve.setStatutSession(statutSessionRepository.findByKey(keyStatut));
    		}else {
    			sessionEpreuve.setStatutSession(sessionEpreuveService.getStatutSession(sessionEpreuve));
    		}
    	}
    	sessionEpreuve.setAnneeUniv(String.valueOf(sessionEpreuveService.getCurrentAnneeUnivFromDate(sessionEpreuve.getDateExamen())));
    	sessionEpreuveService.save(sessionEpreuve, emargementContext, null);
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	log.info("Maj d'une session : " + sessionEpreuve.getNomSessionEpreuve());
    	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), auth.getName(), null, emargementContext, null);
    	return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
 
    }
    
    @PostMapping(value = "/manager/sessionEpreuve/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable Long id, 
    		@RequestParam(required = false)String view, final RedirectAttributes redirectAttributes) {
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
    	if(view!=null) {
    		return String.format("redirect:/%s/manager/sessionEpreuve/old", emargementContext);
    	}
        return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuve.getAnneeUniv());
    }
    
    @Transactional
    @PostMapping(value = "/manager/sessionEpreuve/actions")
    public String actions(@PathVariable String emargementContext, @RequestParam(value = "checkedValues") List<Long> ids, 
    		@RequestParam String action, final RedirectAttributes redirectAttributes) {
    	List<SessionEpreuve> ses = sessionEpreuveRepository.findAllById(ids);
    	if(!ses.isEmpty()) {
    		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    		for (SessionEpreuve se : ses) {
    			String nom = se.getNomSessionEpreuve();
    			if(("delete").equals(action)){
        	    	try {
        				sessionEpreuveService.delete(se);
        				log.info("Suppression d'une session : " + nom);
        				logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + nom, auth.getName(), null, emargementContext, null);
        			} catch (Exception e) {
        				redirectAttributes.addFlashAttribute("error", "constrainttError");
        				log.info("Impossible de supprimer la session : " + nom, e);
        				logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.FAILED, "Nom : " + nom, auth.getName(), null, emargementContext, null);
        			}  
    			}else if(("close").equals(action)){
    				se.setStatutSession(statutSessionRepository.findByKey("CLOSED"));
    	        	sessionEpreuveRepository.save(se);
    			   	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + se.getNomSessionEpreuve() + " : " + "changement statut CLOSED.", auth.getName(), null, emargementContext, null);
        		}
    	    }	
    	}
    	return String.format("redirect:/%s/manager/sessionEpreuve?anneeUniv=%s", emargementContext, sessionEpreuveService.getCurrentanneUniv());
    }
    
    @PostMapping("/manager/sessionEpreuve/emargement")
    public void exportEamrgement(@PathVariable String emargementContext, @RequestParam Long sessionLocationId, 
    			@RequestParam Long sessionEpreuveId, @RequestParam String type, HttpServletResponse response){
    	
    	sessionEpreuveService.exportEmargement(response, sessionLocationId, sessionEpreuveId, type, emargementContext);
    }
    
    @PostMapping("/manager/sessionEpreuve/affinerRepartition/{id}")
    @Transactional
    public String affinerRepartition(@PathVariable String emargementContext, @PathVariable Long id, final RedirectAttributes redirectAttributes, @ModelAttribute(value="form") PropertiesForm  propertiesForm,
    		@RequestParam String tagCheckOrder){
		 sessionEpreuveService.affinageRepartition(propertiesForm, emargementContext, tagCheckOrder);
		 redirectAttributes.addFlashAttribute("tagCheckOrderValue", tagCheckOrder);
    	 return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
    
    @Transactional
    @GetMapping("/manager/sessionEpreuve/duplicate/{id}")
    public String duplicateSession(@PathVariable String emargementContext, @PathVariable Long id, final RedirectAttributes redirectAttributes){
    	SessionEpreuve newSe = sessionEpreuveService.duplicateSessionEpreuve(id, false, "", "");
    	redirectAttributes.addFlashAttribute("duplicate", "duplicate");
    	return String.format("redirect:/%s/manager/sessionEpreuve/%s?form", emargementContext, newSe.getId());
    }
    
    @Transactional
    @PostMapping("/manager/sessionEpreuve/duplicateAll")
    public String duplicateAllSessions(@PathVariable String emargementContext, @RequestParam String jours, @RequestParam String newName,
    		@RequestParam List<Long> idSessions){
    	sessionEpreuveService.duplicateAll(idSessions, jours, newName);
    	return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
    
    @PostMapping("/manager/sessionEpreuve/changeStatut/{id}")
    public String changeStatut(@PathVariable String emargementContext, @PathVariable Long id, 
    		@RequestParam String statut, @RequestParam(required = false) String view, final RedirectAttributes redirectAttributes){
    	
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	sessionEpreuve.setStatutSession(statutSessionRepository.findByKey(statut));
    	sessionEpreuveRepository.save(sessionEpreuve);
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	
    	redirectAttributes.addFlashAttribute("currentAnneeUniv", sessionEpreuve.getAnneeUniv());
    	log.info("Maj d'une session : " + sessionEpreuve.getNomSessionEpreuve());
    	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve() + " : " + "changement statut " + statut, auth.getName(), null, emargementContext, null);
    	if(view!=null) {
    		return String.format("redirect:/%s/manager/sessionEpreuve/old", emargementContext);
    	}
    	return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
    
	@Transactional
    @PostMapping("/manager/sessionEpreuve/updateAde/{id}")
	public String  updateSessionAde(@PathVariable String emargementContext, @PathVariable("id") List<SessionEpreuve> listSe) throws IOException, ParserConfigurationException, SAXException, ParseException, XPathExpressionException {
		if(listSe != null) {
			Context ctx = contextRepository.findByKey(emargementContext);
			adeService.updateSessionEpreuve(listSe, emargementContext, "manual", ctx);
		}
		
		return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
	}
	
    @GetMapping("/manager/sessionEpreuve/old")
    public String cleanup(Model model){
    	Date today = new Date();
    	model.addAttribute("autoClose", appliConfigRepository.findAppliConfigByKey("AUTO_CLOSE_SESSION").get(0).getValue());
    	model.addAttribute("notClosed", sessionEpreuveRepository.findByDateExamenLessThanAndDateFinIsNullAndStatutSessionKeyNotOrDateFinLessThanAndStatutSessionKeyNot(today, "CLOSED", today, "CLOSED"));
    	model.addAttribute("noTagCheck", sessionEpreuveRepository.findSessionEpreuveWithNoTagCheck(new Date(), contexteService.getcurrentContext().getId()));
    	model.addAttribute("noTagChecker", sessionEpreuveRepository.findSessionEpreuveWithNoTagChecker(new Date(), contexteService.getcurrentContext().getId()));
    	model.addAttribute("noSessionLocation", sessionEpreuveRepository.findSessionEpreuveWithNoSessionLocation(new Date(), contexteService.getcurrentContext().getId()));
       	model.addAttribute("noTagDate", sessionEpreuveRepository.findSessionEpreuveWithNoTagDate(new Date(), contexteService.getcurrentContext().getId()));

    	return "manager/sessionEpreuve/old";
    }

    @Transactional
    @PostMapping("/manager/sessionEpreuve/cleanup/{type}")
	public String cleanupsession(@PathVariable String emargementContext, @PathVariable String type){
    	Date today = new Date();
    	if(type!=null) {
    		if("notClosed".equals(type)){
    			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    			for(SessionEpreuve se :  sessionEpreuveRepository.findByDateExamenLessThanAndDateFinIsNullAndStatutSessionKeyNotOrDateFinLessThanAndStatutSessionKeyNot(today, "CLOSED", today, "CLOSED")){
    				se.setStatutSession(statutSessionRepository.findByKey("CLOSED"));
    				sessionEpreuveRepository.save(se);
    			   	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + se.getNomSessionEpreuve() + " : " + "changement statut CLOSED", auth.getName(), null, emargementContext, null);
    			}
    		}else if("noTagCheck".equals(type)){
    			sessionEpreuveService.deleteAll(sessionEpreuveRepository.findSessionEpreuveWithNoTagCheck(new Date(), contexteService.getcurrentContext().getId()));
    		}else if("noTagDate".equals(type)){
    			sessionEpreuveService.deleteAll(sessionEpreuveRepository.findSessionEpreuveWithNoTagDate(new Date(), contexteService.getcurrentContext().getId()));
    		}else if("noTagChecker".equals(type)){
    			sessionEpreuveService.deleteAll(sessionEpreuveRepository.findSessionEpreuveWithNoTagChecker(new Date(), contexteService.getcurrentContext().getId()));
    		}else if("noSessionLocation".equals(type)){
    			sessionEpreuveService.deleteAll(sessionEpreuveRepository.findSessionEpreuveWithNoSessionLocation(new Date(), contexteService.getcurrentContext().getId()));
    		}
    	}
		
		return String.format("redirect:/%s/manager/sessionEpreuve/old", emargementContext);
	}
    
	@Transactional
    @PostMapping("/manager/sessionEpreuve/autoClose")
	public String  activeAutoClose(@PathVariable String emargementContext){
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean  autoCLose = (appliConfigService.getAutoCloseSession())? false : true;
		AppliConfig appliConfig = appliConfigRepository.findAppliConfigByKey("AUTO_CLOSE_SESSION").get(0);
		appliConfig.setValue(String.valueOf(autoCLose));
		appliConfigRepository.save(appliConfig);
		logService.log(ACTION.UPDATE_CONFIG, RETCODE.SUCCESS, "Key : ".concat(appliConfig.getKey()).concat(" value : ").concat(appliConfig.getValue()), auth.getName(), null, emargementContext, null);
		return String.format("redirect:/%s/manager/sessionEpreuve/old", emargementContext);
	}
	
	@Transactional
    @PostMapping(value = "/manager/sessionEpreuve/importCsv", produces = "text/html")
    public String importCsv(@PathVariable String emargementContext, List<MultipartFile> files, final RedirectAttributes redirectAttributes) throws Exception {
    	List<InputStream> streams = new ArrayList<>();
    	for(MultipartFile file : files) {
    		streams.add(file.getInputStream());
    	}
    	SequenceInputStream is = new SequenceInputStream(Collections.enumeration(streams));
    	String bilanCsv = sessionEpreuveService.importSessionsCsv(is, emargementContext);
    	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
    	return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
}
