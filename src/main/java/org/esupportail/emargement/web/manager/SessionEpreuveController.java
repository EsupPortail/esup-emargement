package org.esupportail.emargement.web.manager;

import java.io.IOException;
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

import org.esupportail.emargement.domain.Event;
import org.esupportail.emargement.domain.PropertiesForm;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.EventRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.custom.SessionEpreuveRepositoryCustom;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.EventService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.SessionLocationService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import net.fortuna.ical4j.data.ParserException;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class SessionEpreuveController {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	SessionEpreuveRepositoryCustom sessionEpreuveRepositoryCustom;
	
	@Autowired
	CampusRepository campusRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
    @Resource
    SessionEpreuveService sessionEpreuveService;
    
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
	
	private final static String ITEM = "sessionEpreuve";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/manager/sessionEpreuve")
	public String list(Model model, @PageableDefault(size = 20, direction = Direction.DESC, sort = "dateExamen")  Pageable pageable, @RequestParam(value="seNom", required = false) String seNom, 
			@RequestParam(value="anneeUniv", required = false) String anneeUniv){
		
		if(anneeUniv==null) {
			anneeUniv = String.valueOf(sessionEpreuveService.getCurrentanneUniv());
		}
		
        Page<SessionEpreuve> sessionEpreuvePage = sessionEpreuveRepository.findAllByAnneeUniv(anneeUniv, pageable);
        
        if(seNom!=null) {
        	sessionEpreuvePage = sessionEpreuveRepository.findByNomSessionEpreuve(seNom, pageable);
        	model.addAttribute("seNom", seNom);
        	model.addAttribute("collapse", "show");
        }
        sessionEpreuveService.computeCounters(sessionEpreuvePage.getContent());
        model.addAttribute("currentAnneeUniv", anneeUniv);
        model.addAttribute("years", sessionEpreuveService.getYears());
        model.addAttribute("sessionEpreuvePage", sessionEpreuvePage);
        model.addAttribute("paramUrl", "0");
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
		return "manager/sessionEpreuve/list";
	}
	
	@GetMapping(value = "/manager/sessionEpreuve/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		List<SessionEpreuve> list = new ArrayList<SessionEpreuve>();
		list.add(se);
		sessionEpreuveService.computeCounters(list);
	//	se.setDureeEpreuve(sessionEpreuveService.getDureeEpreuve(se));
        uiModel.addAttribute("sessionEpreuve", list.get(0));
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
    public String executeRepartition(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
		boolean isOver = sessionEpreuveService.executeRepartition(id);
		SessionEpreuve sessionEpreuve =  sessionEpreuveRepository.findById(id).get();
		if(sessionEpreuve.isSessionEpreuveClosed) {
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
    public String createForm(@PathVariable String emargementContext, Model uiModel) throws IOException, ParserException, ParseException {
    	SessionEpreuve SessionEpreuve = new SessionEpreuve();
    	populateEditForm(uiModel, SessionEpreuve);
    	List<Event> icsList = eventRepository.findByIsEnabledTrue();
    	eventService.setNbEvent(icsList);
    	uiModel.addAttribute("icsList", icsList);
    	uiModel.addAttribute("events", eventService.getEventsListFromIcs(emargementContext, eventService.getAllUrlList() ));
        return "manager/sessionEpreuve/create";
    }
    
    @GetMapping(value = "/manager/sessionEpreuve/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	SessionEpreuve SessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	populateEditForm(uiModel, SessionEpreuve);
        return "manager/sessionEpreuve/update";
    }
    
    void populateEditForm(Model uiModel, SessionEpreuve SessionEpreuve) {
    	uiModel.addAttribute("types", sessionEpreuveService.getListTypeSessionEpreuve());
    	uiModel.addAttribute("allCampuses", campusRepository.findAll());
        uiModel.addAttribute("sessionEpreuve", SessionEpreuve);
        uiModel.addAttribute("years", sessionEpreuveService.getYears());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        Date date = Calendar.getInstance().getTime();  
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
        String strDate = dateFormat.format(date);  
        uiModel.addAttribute("minDate", strDate);
    }
    
    @PostMapping("/manager/sessionEpreuve/create")
    public String create(@PathVariable String emargementContext, @Valid SessionEpreuve sessionEpreuve, BindingResult bindingResult, Model uiModel, 
    						HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) throws IOException {
    	
    	int compareEpreuve = toolUtil.compareDate(sessionEpreuve.getFinEpreuve(), sessionEpreuve.getHeureEpreuve(), "HH:mm");
    	int compareConvoc = toolUtil.compareDate(sessionEpreuve.getHeureEpreuve(), sessionEpreuve.getHeureConvocation(), "HH:mm");
    	
    	if (bindingResult.hasErrors() || compareEpreuve<= 0 || compareConvoc<=0) {
            populateEditForm(uiModel, sessionEpreuve);
            uiModel.addAttribute("compareEpreuve", (compareEpreuve<= 0) ? true : false);
            uiModel.addAttribute("compareConvoc", (compareConvoc<= 0) ? true : false);
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
        	logService.log(ACTION.AJOUT_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), ldapService.getEppn(auth.getName()), null, emargementContext, null);
            return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
        }        
    }
    
    @PostMapping("/manager/sessionEpreuve/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid SessionEpreuve sessionEpreuve, BindingResult bindingResult, Model uiModel, 
    					HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) throws IOException {
        
    	int compareEpreuve = toolUtil.compareDate(sessionEpreuve.getFinEpreuve(), sessionEpreuve.getHeureEpreuve(), "HH:mm");
    	int compareConvoc = toolUtil.compareDate(sessionEpreuve.getHeureEpreuve(), sessionEpreuve.getHeureConvocation(), "HH:mm");
    	
    	if (bindingResult.hasErrors() || compareEpreuve<= 0 || compareConvoc<=0) {
            populateEditForm(uiModel, sessionEpreuve);
            uiModel.addAttribute("compareEpreuve", (compareEpreuve<= 0) ? true : false);
            uiModel.addAttribute("compareConvoc", (compareConvoc<= 0) ? true : false);
            return "manager/sessionEpreuve/update";
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
        	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve(), ldapService.getEppn(auth.getName()), null, emargementContext, null);
            return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
        }        
    }
    
    @PostMapping(value = "/manager/sessionEpreuve/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	String nom = sessionEpreuve.getNomSessionEpreuve();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
			sessionEpreuveRepository.delete(sessionEpreuve);
			log.info("Suppression d'une session : " + nom);
			logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + nom, ldapService.getEppn(auth.getName()), null, emargementContext, null);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "constrainttError");
			log.info("Impossible de supprimer la session : " + nom, e);
			logService.log(ACTION.DELETE_SESSION_EPREUVE, RETCODE.FAILED, "Nom : " + nom, ldapService.getEppn(auth.getName()), null, emargementContext, null);
		}
        return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
    
    @PostMapping("/manager/sessionEpreuve/emargement")
    public void exportEamrgement(@PathVariable String emargementContext, @RequestParam("sessionLocationId") Long sessionLocationId, 
    			@RequestParam("sessionEpreuveId") Long sessionEpreuveId, @RequestParam("type") String type, HttpServletResponse response){
    	
    	sessionEpreuveService.exportEmargement(response, sessionLocationId, sessionEpreuveId, type);
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
    public String affinerRepartition(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel,@ModelAttribute(value="form") PropertiesForm  propertiesForm){
		 sessionEpreuveService.affinageRepartition(propertiesForm, emargementContext);
    	 return String.format("redirect:/%s/manager/sessionEpreuve/repartition/%s", emargementContext, id);
    }
    
    @Transactional
    @GetMapping("/manager/sessionEpreuve/duplicate/{id}")
    public String duplicateSession(@PathVariable String emargementContext, @PathVariable("id") Long id, final RedirectAttributes redirectAttributes) throws IOException {
    	
    	SessionEpreuve newSe = sessionEpreuveService.duplicateSessionEpreuve(id);
    	redirectAttributes.addFlashAttribute("duplicate", "duplicate");
    	return String.format("redirect:/%s/manager/sessionEpreuve/%s?form", emargementContext, newSe.getId());
    	
    }
    
    @GetMapping("/manager/sessionEpreuve/close/{id}")
    public String closeSession(@PathVariable String emargementContext, @PathVariable("id") Long id) throws IOException {
    	
    	SessionEpreuve sessionEpreuve = sessionEpreuveRepository.findById(id).get();
    	boolean isClosed = (sessionEpreuve.getIsSessionEpreuveClosed())? false : true;
    	String  msg = (sessionEpreuve.getIsSessionEpreuveClosed())?  "Réouverture" : "Clôture";
    	sessionEpreuve.setIsSessionEpreuveClosed(isClosed);
    	sessionEpreuveRepository.save(sessionEpreuve);
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	log.info("Maj d'une session : " + sessionEpreuve.getNomSessionEpreuve());
    	logService.log(ACTION.UPDATE_SESSION_EPREUVE, RETCODE.SUCCESS, "Nom : " + sessionEpreuve.getNomSessionEpreuve() + " : " + msg, ldapService.getEppn(auth.getName()), null, emargementContext, null);
    	return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    	
    }
}
