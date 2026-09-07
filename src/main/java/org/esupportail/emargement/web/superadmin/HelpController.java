package org.esupportail.emargement.web.superadmin;

import javax.annotation.Resource;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.domain.Help;
import org.esupportail.emargement.repositories.HelpRepository;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.utils.ToolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
@HelpPage("help")
public class HelpController {

	@Autowired
	HelpRepository helpRepository;
	
	@Resource
	LogService logService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "help";
	}
	
	@GetMapping(value = "/superadmin/help")
	public String list(Model model, @PageableDefault(size = 30, direction = Direction.ASC, sort = "key")  Pageable pageable) {
		
		Long count = helpRepository.count();
		
		int size = pageable.getPageSize();
		if( size == 1) {
			size = count.intValue();
		}
		
        Page<Help> helpPage = helpRepository.findAll(toolUtil.updatePageable(pageable, size));
        model.addAttribute("helpPage", helpPage);
        model.addAttribute("selectAll", count);
		return "superadmin/help/list";
	}
	
	@GetMapping(value = "/superadmin/help/{id}", produces = "text/html")
    public String show(@PathVariable Long id, Model uiModel) {
        uiModel.addAttribute("helpItem",  helpRepository.findById(id).get());
        return "superadmin/help/show";
    }
}
