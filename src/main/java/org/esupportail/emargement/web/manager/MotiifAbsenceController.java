package org.esupportail.emargement.web.manager;

import java.util.Date;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.repositories.MotifAbsenceRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class MotiifAbsenceController {
	
	@Autowired
	MotifAbsenceRepository motifAbsenceRepository;
	
	@Autowired
	UserAppRepository userAppRepository;

	@Resource
	ContextService contexteService;
	
	@Resource
	LogService logService;
	
	private final static String ITEM = "motifAbsence";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/motifAbsence")
	public String list(Model uiModel, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable) {
	
		uiModel.addAttribute("motifAbsences", motifAbsenceRepository.findAll(pageable));
		return "manager/motifAbsence/list";
	}
	
    @GetMapping(value = "/manager/motifAbsence", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	MotifAbsence motifAbsence = new MotifAbsence();
    	uiModel.addAttribute("motifAbsence", motifAbsence);
        return "manager/motifAbsence/create";
    }
    
    @Transactional
    @PostMapping("/manager/motifAbsence/create")
    public String create(@PathVariable String emargementContext, @Valid MotifAbsence motifAbsence) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	motifAbsence.setContext(contexteService.getcurrentContext());
    	motifAbsence.setDateModification(new Date());
    	motifAbsence.setUserApp(userAppRepository.findByEppnAndContextKey(auth.getName(), emargementContext));
    	motifAbsenceRepository.save(motifAbsence);
    	logService.log(ACTION.CREATE_MOTIF_ABSENCE, RETCODE.SUCCESS, motifAbsence.getLibelle(), auth.getName(), null, emargementContext, null);
    	return String.format("redirect:/%s/manager/motifAbsence", emargementContext);
    }
    
    @GetMapping(value = "/manager/motifAbsence/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") MotifAbsence motifAbsence, Model uiModel) {
    	uiModel.addAttribute("motifAbsence", motifAbsence);
        return "manager/motifAbsence/update";
    }
    
    @PostMapping("/manager/motifAbsence/update/{id}")
    public String update(@PathVariable String emargementContext, @Valid MotifAbsence motifAbsence) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	motifAbsence.setContext(contexteService.getcurrentContext());
    	motifAbsence.setDateModification(new Date());
    	motifAbsence.setUserApp(userAppRepository.findByEppnAndContextKey(auth.getName(), emargementContext));
    	motifAbsenceRepository.save(motifAbsence);
    	logService.log(ACTION.UPDATE_MOTIF_ABSENCE, RETCODE.SUCCESS, motifAbsence.getLibelle(), auth.getName(), null, emargementContext, null);
    	return String.format("redirect:/%s/manager/motifAbsence", emargementContext);
    }
    
    @Transactional
    @PostMapping(value = "/manager/motifAbsence/{id}")
    public String delete(@PathVariable String emargementContext,  @PathVariable("id")  MotifAbsence motifAbsence) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	motifAbsenceRepository.delete(motifAbsence);
    	logService.log(ACTION.DELETE_MOTIF_ABSENCE, RETCODE.SUCCESS, motifAbsence.getLibelle(), auth.getName(), null, emargementContext, null);
    	return String.format("redirect:/%s/manager/motifAbsence", emargementContext);
    }
}
