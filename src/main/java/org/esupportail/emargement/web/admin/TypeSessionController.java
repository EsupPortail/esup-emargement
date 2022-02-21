package org.esupportail.emargement.web.admin;

import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.TypeSession;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.TypeSessionService;
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
@PreAuthorize(value="@userAppService.isAdmin()")
public class TypeSessionController {

	private final static String ITEM = "typeSession";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	TypeSessionRepository typeSessionRepository;
	
	@Autowired
	TypeSessionService typeSessionService;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	HelpService helpService;
	
	@Resource
	LogService logService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/typeSession")
	public String list(@PathVariable String emargementContext, Model model, @PageableDefault(size = 20, direction = Direction.ASC, sort = "key")  Pageable pageable) {
		
        Page<TypeSession> typeSessionPage = typeSessionRepository.findAll(pageable);
        //Context context = contextRepository.findByContextKey(emargementContext);
        model.addAttribute("typeSessionPage", typeSessionPage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("checkTypeSession", typeSessionService.checkTypeSession());
		return "admin/typeSession/list";
	}
	
	@GetMapping(value = "/admin/typeSession/updateTypes")
	public String updateTypes(@PathVariable String emargementContext, Model model, final RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("nbUpdate", typeSessionService.updateTypeSession(emargementContext));
		return String.format("redirect:/%s/admin/typeSession", emargementContext);
	}
	
    @GetMapping(value = "/admin/typeSession", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	TypeSession typeSession = new TypeSession();
    	uiModel.addAttribute("typeSession", typeSession);
        return "admin/typeSession/create";
    }
    
    @GetMapping(value = "/admin/typeSession/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	TypeSession typeSession = typeSessionRepository.findById(id).get();
    	uiModel.addAttribute("typeSession", typeSession);
        return "admin/typeSession/update";
    }
    
    @PostMapping("/admin/typeSession/create")
    public String create(@PathVariable String emargementContext, @Valid TypeSession typeSession, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("typeSession", typeSession);
            return "admin/typeSession/create";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(typeSessionRepository.countByKey(typeSession.getKey())>0) {
        	redirectAttributes.addFlashAttribute("key", typeSession.getKey());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, code session déjà existant : " + "Key : ".concat(typeSession.getKey()));
        	return String.format("redirect:/%s/admin/typeSession?form", emargementContext);
        }else {
        	typeSession.setDateModification(new Date());
        	typeSession.setAddByAdmin(false);
        	Context context = contextRepository.findByKey(emargementContext);
        	typeSession.setContext(context);
        	typeSessionRepository.save(typeSession);
            log.info("Création type de session : " + "Key : ".concat(typeSession.getKey()));
            logService.log(ACTION.AJOUT_TYPESESSION, RETCODE.SUCCESS, "Key : ".concat(typeSession.getKey()).concat(" libellé : ").concat(typeSession.getLibelle()), auth.getName(), null,
            		emargementContext, null);
            return String.format("redirect:/%s/admin/typeSession", emargementContext);
        }
    }
    
    @PostMapping("/admin/typeSession/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid TypeSession typeSession, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("typeSession", typeSession);
            return "superadmin/help/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Context context = contextRepository.findByKey(emargementContext);
    	typeSession.setContext(context);
        typeSession.setDateModification(new Date());
        typeSessionRepository.save(typeSession);
        log.info("Maj type de session : " + "Key : ".concat(typeSession.getKey()));
        logService.log(ACTION.UPDATE_TYPESESSION, RETCODE.SUCCESS, "Key : ".concat(typeSession.getKey()).concat(" libellé : ").concat(typeSession.getLibelle()), auth.getName(), null,
        		emargementContext, null);
        return String.format("redirect:/%s/admin/typeSession", emargementContext);
    }
    
	@GetMapping(value = "/admin/typeSession/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("typeSession",  typeSessionRepository.findById(id).get());
        return "admin/typeSession/show";
    }
	
    @PostMapping(value = "/admin/typeSession/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	TypeSession typeSession = typeSessionRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
    		typeSessionRepository.delete(typeSession);
			log.info("Suppression type de session : " + "Key : ".concat(typeSession.getKey()).concat(typeSession.getLibelle()));
	        logService.log(ACTION.DELETE_TYPESESSION, RETCODE.SUCCESS, "Key : ".concat(typeSession.getKey()).concat(" libellé : ").concat(typeSession.getLibelle()), auth.getName(), null,
	        		emargementContext, null);
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("typeSession", typeSession.getKey());
			redirectAttributes.addFlashAttribute("error", "constrainttError");
		}
    	return String.format("redirect:/%s/admin/typeSession", emargementContext);
    }

}
