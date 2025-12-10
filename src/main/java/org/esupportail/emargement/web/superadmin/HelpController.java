package org.esupportail.emargement.web.superadmin;

import java.util.Date;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Help;
import org.esupportail.emargement.repositories.HelpRepository;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ToolUtil;
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
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class HelpController {

	private final static String ITEM = "help";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	HelpRepository helpRepository;
	
	@Resource
	LogService logService;
	
	@Resource
	HelpService helpService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/superadmin/help")
	public String list(Model model, @PageableDefault(size = 30, direction = Direction.ASC, sort = "key")  Pageable pageable) {
		
		Long count = helpRepository.count();
		
		int size = pageable.getPageSize();
		if( size == 1) {
			size = count.intValue();
		}
		
        Page<Help> helpPage = helpRepository.findAll(toolUtil.updatePageable(pageable, size));
        model.addAttribute("checkHelp", helpService.checkHelp());
        model.addAttribute("helpPage", helpPage);
        model.addAttribute("help", helpService.getValueOfKey(ITEM));
        model.addAttribute("selectAll", count);
		return "superadmin/help/list";
	}
	
    void populateEditForm(Model uiModel, Help help) {
    	uiModel.addAttribute("categories", helpService.getHelpCategories());
        uiModel.addAttribute("help", help);
    }
	
    @GetMapping(value = "/superadmin/help", params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	Help help = new Help();
    	populateEditForm(uiModel, help);
        return "superadmin/help/create";
    }
    
    @PostMapping("/superadmin/help/create")
    public String create(@PathVariable String emargementContext, @Valid Help help, BindingResult bindingResult, Model uiModel, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, help);
            return "superadmin/help/create";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(helpRepository.countByKey(help.getKey())>0) {
        	redirectAttributes.addFlashAttribute("key",help.getKey());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, aide déjà existante : " + "Key : ".concat(help.getKey()));
        	return String.format("redirect:/%s/superadmin/help?form", emargementContext);
        }
		help.setDateModification(new Date());
		helpRepository.save(help);
		log.info("Création aide : " + "Key : ".concat(help.getKey()));
		logService.log(ACTION.AJOUT_HELP, RETCODE.SUCCESS, "Key : ".concat(help.getKey()).concat(" value : ").concat(help.getValue()), auth.getName(), null,
				emargementContext, null);
		return String.format("redirect:/%s/superadmin/help", emargementContext);
    }
    
	@GetMapping(value = "/superadmin/help/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel) {
        uiModel.addAttribute("help",  helpRepository.findById(id).get());
        return "superadmin/help/show";
    }

    @GetMapping(value = "/superadmin/help/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable Long id, Model uiModel) {
    	Help help = helpRepository.findById(id).get();
    	populateEditForm(uiModel, help);
        return "superadmin/help/update";
    }
    
    
    @PostMapping("/superadmin/help/update/{id}")
    public String update(@PathVariable String emargementContext, @Valid Help help, BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, help);
            return "superadmin/help/update";
        }
        uiModel.asMap().clear();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        help.setDateModification(new Date());
        helpRepository.save(help);
        log.info("Maj aide : " + "Key : ".concat(help.getKey()).concat(help.getValue()));
        logService.log(ACTION.UPDATE_HELP, RETCODE.SUCCESS, "Key : ".concat(help.getKey()).concat(" value : ").concat(help.getValue()), auth.getName(), null,
        		emargementContext, null);
        return String.format("redirect:/%s/superadmin/help", emargementContext);
    }
    
    @PostMapping(value = "/superadmin/help/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable Long id, final RedirectAttributes redirectAttributes) {
    	Help help = helpRepository.findById(id).get();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	try {
			helpRepository.delete(help);
			log.info("Suppression aide : " + "Key : ".concat(help.getKey()).concat(help.getValue()));
	        logService.log(ACTION.DELETE_HELP, RETCODE.SUCCESS, "Key : ".concat(help.getKey()).concat(" value : ").concat(help.getValue()), auth.getName(), null,
	        		emargementContext, null);
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("help", help.getKey());
			redirectAttributes.addFlashAttribute("error", "constrainttError");
		}
    	return String.format("redirect:/%s/superadmin/help", emargementContext);
    }
    
	@GetMapping(value = "/superadmin/help/updateHelps")
	public String updateHelps(@PathVariable String emargementContext, final RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("nbUpdate", helpService.updateHelp());
		return String.format("redirect:/%s/superadmin/help", emargementContext);
	}
}
