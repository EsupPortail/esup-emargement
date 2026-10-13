package org.esupportail.emargement.web.admin;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.annotations.HelpPage;
import org.esupportail.emargement.beans.ExportResult;
import org.esupportail.emargement.domain.Archive;
import org.esupportail.emargement.services.ArchiveService;
import org.esupportail.emargement.services.TagCheckService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<byte[]> exportTagChecks(@PathVariable String emargementContext,
			@RequestParam String anneeUniv, @RequestParam String type) {

		ExportResult result = tagCheckService.exportTagChecks(type, null, emargementContext, anneeUniv);
		String disposition = tagCheckService.buildContentDisposition(result.getFilename());

		// CSV : UTF-8 BOM pour Excel
		if ("CSV".equals(type)) {
			byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			byte[] csv = result.getBytes();
			byte[] body = new byte[bom.length + csv.length];
			System.arraycopy(bom, 0, body, 0, bom.length);
			System.arraycopy(csv, 0, body, bom.length, csv.length);
			return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
					.header(HttpHeaders.CONTENT_DISPOSITION, disposition).body(body);
		}

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition).body(result.getBytes());
	}
	
	@PostMapping("/admin/archives/anonymize")
	public String archiverInscrits(@PathVariable String emargementContext, @RequestParam String anneeUniv, @RequestParam boolean booleanAnonymize) {
		if(booleanAnonymize) {
			tagCheckService.archiverTagChecks(anneeUniv, emargementContext);
		}
		return String.format("redirect:/%s/admin/archives/" , emargementContext);
	}
}
