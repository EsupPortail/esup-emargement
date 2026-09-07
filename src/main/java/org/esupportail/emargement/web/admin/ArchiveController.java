package org.esupportail.emargement.web.admin;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.domain.Archive;
import org.esupportail.emargement.services.ArchiveService;
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
@HelpPage("archives")
public class ArchiveController {
	
	@Resource
	ArchiveService archiveService;
	
	@Resource
	TagCheckService tagCheckService;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return "archives";
	}
	
	@GetMapping(value = "/admin/archives")
	public String list(@PathVariable String emargementContext, Model model){
		List<Archive> archives = archiveService.getArchivesList(emargementContext);
		model.addAttribute("archives", archives);
		return "admin/archives";
	}
	
	@PostMapping("/admin/archives/export")
    public void exportTagChecks(@PathVariable String emargementContext, @RequestParam String anneeUniv, @RequestParam String type, HttpServletResponse response){
    	
    	tagCheckService.exportTagChecks(type, null, emargementContext, anneeUniv);
    }
	
	@PostMapping("/admin/archives/anonymize")
	public String archiverInscrits(@PathVariable String emargementContext, @RequestParam String anneeUniv, @RequestParam boolean booleanAnonymize) {
		if(booleanAnonymize) {
			tagCheckService.archiverTagChecks(anneeUniv, emargementContext);
		}
		return String.format("redirect:/%s/admin/archives/" , emargementContext);
	}
}
