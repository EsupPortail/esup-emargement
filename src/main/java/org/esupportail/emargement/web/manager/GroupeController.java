package org.esupportail.emargement.web.manager;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.GroupeService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class GroupeController {
	
	@Autowired
	GroupeRepository groupeRepository;

	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	LogService logService;
	
	@Resource
	GroupeService groupeService;
	
	@Resource
	HelpService helpService;
	
	@Resource
	LdapService ldapService;
	
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
        	String eppn = ldapService.getEppn(auth.getName());
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
        	String eppn = ldapService.getEppn(auth.getName());
        	oldGroupe.setDescription(groupe.getDescription());
        	oldGroupe.setNom(groupe.getNom());
        	oldGroupe.setModificateur(eppn);
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
    		List<TagCheck> tcs = tagCheckRepository.findTagCheckByGroupeId(id);
    		for(TagCheck tc : tcs) {
    			tc.setGroupe(null);
    		}
    		groupeRepository.delete(groupe);
	        log.info("suppression groupe : " + nom);
	        logService.log(ACTION.DELETE_GROUPE, RETCODE.SUCCESS, null, ldapService.getEppn(auth.getName()), null, emargementContext, null);
		} catch (Exception e) {
	        log.info("suppression du groupe impossible  : " + nom, e);
	        logService.log(ACTION.DELETE_GROUPE, RETCODE.FAILED,  null, ldapService.getEppn(auth.getName()), null, emargementContext, null);
		}
        return String.format("redirect:/%s/manager/groupe", emargementContext);
    }
}
