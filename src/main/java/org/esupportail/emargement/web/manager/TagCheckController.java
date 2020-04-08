package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.services.ApogeeService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.utils.PdfGenaratorUtil;
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

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class TagCheckController {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	PersonService personService;
	
	@Autowired
	PdfGenaratorUtil pdfGenaratorUtil;
	
	@Resource
	ApogeeService apogeeService;
	
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "tagCheck";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	@Autowired
	ToolUtil toolUtil;
	
	@GetMapping(value = "/manager/tagCheck/sessionEpreuve/{id}", produces = "text/html")
    public String listTagChecknBySessionEpreuve(@PathVariable("id") Long id, Model model, 
    		@PageableDefault(size = 1, direction = Direction.ASC, sort = "person")  Pageable pageable, 
    			@RequestParam(defaultValue = "",value="tempsAmenage") String tempsAmenage, @RequestParam(defaultValue = "",value="eppn") String eppn, @RequestParam(value="repartition", required = false) 
    			Long repartitionId) {
		
		Long count = tagCheckService.countTagchecks(tempsAmenage, eppn, id, repartitionId);
		int size = pageable.getPageSize();
		if( size == 1 && count>0) {
			size = count.intValue();
		}
        Page<TagCheck> tagCheckPage = tagCheckService.getTagCheckPage(tempsAmenage, eppn, id, repartitionId, toolUtil.updatePageable(pageable, size));
        int notInLdap = tagCheckService.setNomPrenomTagChecks(tagCheckPage.getContent());
        
		String collapse ="";
		if(!eppn.isEmpty() || repartitionId !=null || !tempsAmenage.isEmpty()) {
			collapse = "show";
		}
        model.addAttribute("tagCheckPage", tagCheckPage);
        model.addAttribute("isSessionEpreuveClosed", sessionEpreuveRepository.findById(id).get().isSessionEpreuveClosed);
		model.addAttribute("paramUrl", String.valueOf(id));
		model.addAttribute("countTagChecks", count);
		model.addAttribute("tempsAmenage",tempsAmenage);
		model.addAttribute("sessionEpreuve", sessionEpreuveRepository.findById(id).get());
		model.addAttribute("sid",id);
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("listeRepartition", tagCheckService.searchRepartition(id));
		model.addAttribute("repartitionId", repartitionId);
		model.addAttribute("eppn", eppn);
		model.addAttribute("collapse", collapse);

		model.addAttribute("countRepartition", tagCheckRepository.countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNull(id));
		model.addAttribute("countConvocations", tagCheckRepository.countTagCheckBySessionEpreuveIdAndDateEnvoiConvocationIsNull(id));
		model.addAttribute("countCheckedBycard",tagCheckRepository.countTagCheckBySessionEpreuveIdAndIsCheckedByCardFalse(id));
		model.addAttribute("selectAll", count);
		model.addAttribute("notInLdap", notInLdap);
        return "manager/tagCheck/list";
    }
	
	@GetMapping(value = "/manager/tagCheck/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
		List<TagCheck> tagChecks = new ArrayList<TagCheck>();
		tagChecks.add( tagCheckRepository.findById(id).get());
		tagCheckService.setNomPrenomTagChecks(tagChecks);
        uiModel.addAttribute("tagCheck", tagChecks.get(0));
        return "manager/tagCheck/show";
    }
	
    @GetMapping(value = "/manager/tagCheck", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	TagCheck TagCheck = new TagCheck();
    	populateEditForm(uiModel, TagCheck);
        return "manager/tagCheck/create";
    }
    
    @GetMapping(value = "/manager/tagCheck/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	TagCheck TagCheck = tagCheckRepository.findById(id).get();
    	populateEditForm(uiModel, TagCheck);
        return "manager/tagCheck/update";
    }
    
    void populateEditForm(Model uiModel, TagCheck TagCheck) {
    	uiModel.addAttribute("allPersons", personRepository.findAll());
    	uiModel.addAttribute("allTagCheckers", tagCheckerRepository.findAll());
    	uiModel.addAttribute("allSessionEpreuves", sessionEpreuveRepository.findAll());
    	uiModel.addAttribute("allSessionLocations", sessionLocationRepository.findAll());
    	uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        uiModel.addAttribute("tagCheck", TagCheck);
    }
    
    @PostMapping("/manager/tagCheck/create")
    @Transactional
    public String create(@PathVariable String emargementContext, @Valid TagCheck tagCheck, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest,  
    		final RedirectAttributes redirectAttributes) throws Exception {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, tagCheck);
            return "manager/tagCheck/create";
        }
        uiModel.asMap().clear();
        List<List<String>> finalList = tagCheckService.setAddList(tagCheck);
    	List<TagCheck> bilanCsv =  tagCheckService.importTagCheckCsv(null, finalList, tagCheck.getSessionEpreuve().getId(), emargementContext);
    	redirectAttributes.addFlashAttribute("paramUrl", tagCheck.getSessionEpreuve());
    	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
    	return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/%s", emargementContext, tagCheck.getSessionEpreuve().getId());
    }
    
    @PostMapping("/manager/tagCheck/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid TagCheck tagCheck, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, tagCheck);
            return "manager/tagCheck/update";
        }
        uiModel.asMap().clear();
        TagCheck tc = tagCheckRepository.findById(id).get();
    	if(tc.getSessionEpreuve().isSessionEpreuveClosed) {
	        log.info("Maj de l'inscrit impossible car la session est cloturée : " + tagCheck.getPerson().getEppn());
    	}else {
    		tc.setContext(contexteService.getcurrentContext());
    		tc.setIsTiersTemps(tagCheck.getIsTiersTemps());
    		tc.setComment(tagCheck.getComment());
    		tagCheckService.save(tc, emargementContext);
    	}
        return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/" + tc.getSessionEpreuve().getId(), emargementContext);
    }
    
    @PostMapping(value = "/manager/tagCheck/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
    	TagCheck tagCheck = tagCheckRepository.findById(id).get();
    	if(tagCheck.getSessionEpreuve().isSessionEpreuveClosed) {
	        log.info("Maj de l'inscrit impossible car la session est cloturée : " + tagCheck.getPerson().getEppn());
    	}else {
    		tagCheckRepository.delete(tagCheck);
    	}
        return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/" + tagCheck.getSessionEpreuve().getId(), emargementContext);
    }
    
	@Transactional
	@GetMapping(value = "/manager/tagCheck/deleteAllTagChecks/{id}", produces = "text/html")
    public String deleteRepartition(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
		tagCheckService.deleteAllTagChecksBySessionEpreuveId(id);
        return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
	
    @PostMapping("/manager/tagCheck/convocationForm")
    @Transactional
    public String convocationForm(@PathVariable String emargementContext, @RequestParam(value = "listeIds", required = false) List<Long> listeIds, @RequestParam(value = "sessionEpreuveId") SessionEpreuve sessionEpreuve, 
    		@RequestParam(value = "submit") String submit, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
    	
    	
    	if("selected".equals(submit) && listeIds!= null && listeIds.isEmpty()) {
    		 redirectAttributes.addFlashAttribute("msgModal", "Vous n'avez pas sélectionné d'inscrits");
    		 return String.format("redirect:/%s/manager/tagCheck/sessionEpreuve/%s", emargementContext, sessionEpreuve.getId());
    		
    	}else {
	    	uiModel.addAttribute("listeIds", listeIds);
	    	uiModel.addAttribute("all", ("all".equals(submit)) ? true : false);
	    	uiModel.addAttribute("sessionEpreuve", sessionEpreuve);
	    	uiModel.addAttribute("tagChecks", tagCheckService.snTagChecks(listeIds));
	    	uiModel.addAttribute("isSendEmails",appliConfigService.isSendEmails());
	    	uiModel.addAttribute("convocationHtml", appliConfigService.getConvocationContenu());
	    	uiModel.addAttribute("sujetMailConvocation", appliConfigService.getConvocationSujetMail());
	    	uiModel.addAttribute("bodyMailConvocation", appliConfigService.getConvocationBodyMail());
	    	uiModel.addAttribute("help", helpService.getValueOfKey("convocation"));
	        return "manager/tagCheck/convocation";
	        
    	}
    }
    
    @PostMapping("/manager/tagCheck/pdfConvocation")
	public void getPdfConvocation(HttpServletResponse response, @RequestParam("htmltemplate") String htmltemplate) throws Exception {
    	tagCheckService.getPdfConvocation(response,htmltemplate);
	}
    
    @PostMapping("/manager/tagCheck/export")
    public void exportTagChecks(@PathVariable String emargementContext,@RequestParam("type") String type, @RequestParam("sessionId") Long id, 
    		@RequestParam("tempsAmenage") String tempsAmenage, HttpServletResponse response){
    	
    	tagCheckService.exportTagChecks(type, id, tempsAmenage, response, emargementContext);
    }
    
	@Transactional
	@PostMapping(value = "/manager/tagCheck/sendConvocation", produces = "text/html")
    public String sendConvocation(@PathVariable String emargementContext, @RequestParam("subject") String subject, @RequestParam("bodyMsg") String bodyMsg, 
    		@RequestParam(value="isSendToManager", defaultValue = "false") boolean isSendToManager,  @RequestParam(value="all", defaultValue = "false") boolean isAll,
    		@RequestParam(value = "listeIds", defaultValue = "") List<Long> listeIds,  @RequestParam("htmltemplatePdf") String htmltemplatePdf, @RequestParam("seId") 
    		Long seId, Model uiModel) throws Exception {

		if(appliConfigService.isSendEmails()){
			tagCheckService.sendEmailConvocation(subject, bodyMsg, isSendToManager, listeIds, htmltemplatePdf, emargementContext, isAll, seId);
		}else {
			log.info("Envoi de mail désactivé :  ");
		}
		
		return String.format("redirect:/%s/manager/sessionEpreuve", emargementContext);
    }
	
    @GetMapping("/manager/tagCheck/searchTagCheck")
    @ResponseBody
    public List<Person> searchLdap(@RequestParam("searchValue") String searchValue, @RequestParam("sessionId") Long sessionId) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	List<Person> persons = new ArrayList<Person>();
    	List<TagCheck>  tagChecksList = tagCheckRepositoryCustom.findAll(searchValue, sessionId);
    	if(!tagChecksList.isEmpty()) {
    		tagCheckService.setNomPrenomTagChecks(tagChecksList);
    		for(TagCheck tc : tagChecksList) {
    			persons.add(tc.getPerson());
    		}
    	}
        return persons;
    }
    
}
