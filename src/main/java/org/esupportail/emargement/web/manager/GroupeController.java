package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.emargement.domain.AppUser;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.GroupeService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class GroupeController {
	
	@Autowired
	GroupeRepository groupeRepository;

	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Resource
	LogService logService;
	
	@Resource
	GroupeService groupeService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "groupe";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/groupe")
	public String list(Model model, @PageableDefault(size = 20, direction = Direction.DESC, sort = "nom") Pageable pageable){
		
		Page<Groupe> groupePage = groupeRepository.findAll(pageable);
		groupeService.computeCounters(groupePage.getContent());
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("groupePage", groupePage);
		return "manager/groupe/list"; 
	}

	@GetMapping(value = "/manager/groupe/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
		Groupe groupe =  groupeRepository.findById(id).get();
		List <Groupe> grs = new ArrayList<Groupe>();
		grs.add(groupe);
		groupeService.computeCounters(grs);
        uiModel.addAttribute("groupe",  groupeRepository.findById(id).get());
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "manager/groupe/show";
    }
	
    @GetMapping(value = "/manager/groupe", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Groupe groupe = new Groupe();
    	populateEditForm(uiModel, groupe);
        return "manager/groupe/create";
    }
	
    @GetMapping(value = "/manager/groupe/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Groupe groupe = groupeRepository.findById(id).get();
    	populateEditForm(uiModel, groupe);
        return "manager/groupe/update";
    }
    
    void populateEditForm(Model uiModel, Groupe groupe) {
    	uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        uiModel.addAttribute("groupe", groupe);
    }
    
    @PostMapping("/manager/groupe/create")
    public String create(@PathVariable String emargementContext, @Valid Groupe groupe, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, 
    		final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, groupe);
            return "/manager/groupe/create";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(groupeRepository.countByNom(groupe.getNom())>0) {
        	redirectAttributes.addFlashAttribute("nom", groupe.getNom());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj, groupe déjà existant : " + groupe.getNom());
        	return String.format("redirect:/%s/manager/groupe/%s?form", emargementContext, groupe.getId());
        }else {
        	groupe.setDateCreation(new Date());
        	String eppn = auth.getName();
        	groupe.setModificateur(eppn);
        	groupe.setContext(contexteService.getcurrentContext());
            groupeRepository.save(groupe);
            log.info("Création groupe : " + groupe.getNom());
            logService.log(ACTION.AJOUT_GROUPE, RETCODE.SUCCESS, "Groupe : ".concat(groupe.getNom()), eppn, null, emargementContext, null);
            return String.format("redirect:/%s/manager/groupe", emargementContext);
        }  
    }
	
    @PostMapping("/manager/groupe/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid Groupe groupe, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, 
    		final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, groupe);
            return "/manager/groupe/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Groupe oldGroupe = groupeRepository.findById(id).get();
        if(groupeRepository.countByNom(groupe.getNom())>0 && !groupe.getNom().equalsIgnoreCase(oldGroupe.getNom())) {
        	redirectAttributes.addFlashAttribute("nom", groupe.getNom());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj, groupe déjà existant : " + groupe.getNom());
        	return String.format("redirect:/%s/manager/groupe/%s?form", emargementContext, groupe.getId());
        }else {
        	oldGroupe.setDateModification(new Date());
        	String eppn = auth.getName();
        	oldGroupe.setDescription(groupe.getDescription());
        	oldGroupe.setNom(groupe.getNom());
        	oldGroupe.setModificateur(eppn);
        	oldGroupe.setAnneeUniv(groupe.getAnneeUniv());
            groupeRepository.save(oldGroupe);
            log.info("Maj groupe : " + groupe.getNom());
            logService.log(ACTION.UPDATE_GROUPE, RETCODE.SUCCESS, "Groupe : ".concat(groupe.getNom()), eppn, null, emargementContext, null);
            return String.format("redirect:/%s/manager/groupe", emargementContext);
        }  
    }
    
    @Transactional
    @PostMapping(value = "/manager/groupe/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Groupe groupe  = groupeRepository.findById(id).get();
		String nom = groupe.getNom();
    	try {
    		groupeService.delete(groupe);
	        log.info("suppression groupe : " + nom);
	        logService.log(ACTION.DELETE_GROUPE, RETCODE.SUCCESS, null, auth.getName(), null, emargementContext, null);
		} catch (Exception e) {
	        log.info("suppression du groupe impossible  : " + nom, e);
	        logService.log(ACTION.DELETE_GROUPE, RETCODE.FAILED,  null, auth.getName(), null, emargementContext, null);
		}
        return String.format("redirect:/%s/manager/groupe", emargementContext);
    }
    
    @GetMapping(value = {"/manager/groupe/addMembers", "/manager/groupe/addMembers/{tab}"})
    public String addToGroupe(@PathVariable String emargementContext, Model uiModel, @PathVariable(value="tab", required = false) String tab) {
    	
    	String type = "";
    	if(tab == null) {
    		type= "user";
    	}else if("user".equals(tab)) {
    		type= "user";
    	}else if("session".equals(tab)) {
    		type= "session";
        	List<SessionEpreuve> allSessionEpreuves = sessionEpreuveRepository.findAllByDateArchivageIsNullOrderByNomSessionEpreuve();
        	sessionEpreuveService.addNbInscrits(allSessionEpreuves);
        	uiModel.addAttribute("allSessionEpreuves",  allSessionEpreuves);
    	}else if("groupe".equals(tab)) {
    		type= "groupe";
    	}
    	uiModel.addAttribute("type",  type);
    	List<Groupe> groupes = groupeRepository.findAllByOrderByNom();
    	groupeService.computeCounters(groupes);
    	
    	uiModel.addAttribute("groupes", groupes) ;
    	
    	return "manager/groupe/addMembers";
    }
    
    @PostMapping(value = "/manager/groupe/addMember")
    public String addOneMember(@PathVariable String emargementContext, @RequestParam(value="eppnTagCheck", required = true) String eppnTagCheck,  
    		@RequestParam(value="groupes") List<Long> groupeIds){
    	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
    	groupeService.addMember(eppnTagCheck, groupeIds);
    	logService.log(ACTION.UPDATE_GROUPE, RETCODE.SUCCESS, "UtILISATEUR -> Groupe(s) : ".concat(groupeService.getNomFromGroupes(groupeIds)), eppn, null, emargementContext, null);

    	return String.format("redirect:/%s/manager/groupe", emargementContext);
    }
    
    @PostMapping(value = "/manager/groupe/addMembersFromSession")
    public String addMemberFromSession(@PathVariable String emargementContext, @RequestParam(value="seIds", required = true)  List<Long> seIds,  
    		@RequestParam(value="groupeIds") List<Long> groupeIds){
    	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
    	groupeService.addMembersFromSessionEpreuve(seIds, groupeIds);
    	logService.log(ACTION.UPDATE_GROUPE, RETCODE.SUCCESS, "SESSION -> Groupe(s) : ".concat(groupeService.getNomFromGroupes(groupeIds)), eppn, null, emargementContext, null);

    	return String.format("redirect:/%s/manager/groupe", emargementContext);
    }
    
    
    @GetMapping(value = "/manager/groupe/seeMembers/{id}")
    public String seeMembers(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, 
    		@PageableDefault(size = 20, direction = Direction.DESC, sort = "person") Pageable pageable) {

    	Page<AppUser> page = new PageImpl<>(groupeService.getMembers(id));
    	uiModel.addAttribute("tagCheckPage" , page);
    	uiModel.addAttribute("count" , page.getSize());
    	uiModel.addAttribute("groupe" , groupeRepository.findById(id).get());
    	return "manager/groupe/seeMembers";
    }
    
    @PostMapping(value = "/manager/groupe/addMembersFromGroupe")
    public String addMemberFromGroupe(@PathVariable String emargementContext, @RequestParam(value="groupeIds", required = true)  List<Long> gr1Ids,  
    		@RequestParam(value="groupeIds2") List<Long> gr2Ids){
    	
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
    	
    	groupeService.addMembersFromGroupe(gr1Ids, gr2Ids);
    	logService.log(ACTION.UPDATE_GROUPE, RETCODE.SUCCESS, "GROUPE -> Groupe(s) : ".concat(groupeService.getNomFromGroupes(gr1Ids)), eppn, null, emargementContext, null);

    	return String.format("redirect:/%s/manager/groupe", emargementContext);
    }
    
    @PostMapping(value = "/manager/groupe/removeTagChecks/{id}")
    public String deleteMemberFromGroupe(@PathVariable String emargementContext, @PathVariable("id") Long id, 
    		@RequestParam(value="case", required = false) List<String> keys, final RedirectAttributes redirectAttributes) {
    	if(keys != null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String eppn = auth.getName();
	    	
	    	Groupe groupe = groupeRepository.findById(id).get();
	    	
	    	groupeService.deleteMembers(keys, groupe);
	    	logService.log(ACTION.UPDATE_GROUPE, RETCODE.SUCCESS, "Suppression membres -> Groupe : ".concat(groupe.getNom()), eppn, null, emargementContext, null);
	    	return String.format("redirect:/%s/manager/groupe", emargementContext);
    	}else {
    		redirectAttributes.addFlashAttribute("error", "noSelection");
    		return String.format("redirect:/%s/manager/groupe/seeMembers/" + id, emargementContext);
    	}
    	
    }
}
