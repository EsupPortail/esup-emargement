package org.esupportail.emargement.web.admin;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.Archive;
import org.esupportail.emargement.services.ArchiveService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.TagCheckService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin()")
public class ArchiveController {
	
	@Resource
	HelpService helpService;
	
	@Resource
	ArchiveService archiveService;
	
	@Resource
	TagCheckService tagCheckService;
	
	private final static String ITEM = "archives";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/archives")
	public String list(@PathVariable String emargementContext, Model model){
		List<Archive> archives = archiveService.getArchivesList(emargementContext);
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("archives", archives);
		return "admin/archives";
	}
	
	@PostMapping("/admin/archives/export")
    public void exportTagChecks(@PathVariable String emargementContext, @RequestParam("anneeUniv") String anneeUniv, @RequestParam("type") String type, HttpServletResponse response){
    	
    	tagCheckService.exportTagChecks(type, null, response, emargementContext, anneeUniv, false);
    }
	
	@PostMapping("/admin/archives/anonymize")
	public String archiverInscrits(@PathVariable String emargementContext, @RequestParam("anneeUniv") String anneeUniv, @RequestParam("booleanAnonymize") boolean booleanAnonymize) {
		if(booleanAnonymize) {
			tagCheckService.archiverTagChecks(anneeUniv, emargementContext);
		}
		return String.format("redirect:/%s/admin/archives/" , emargementContext);
	}
}
