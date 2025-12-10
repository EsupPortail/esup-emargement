package org.esupportail.emargement.web.manager;

import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.EsupSignature.StatutSignature;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.services.EsupSignatureService;
import org.esupportail.emargement.services.StoredFileService;
import org.esupportail.emargement.services.TagCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class EsupSignatureController {
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource	
	EsupSignatureService esupSignatureService;

	@Resource
	StoredFileService storedFileService;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired	
	EsupSignatureRepository esupSignatureRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Value("${emargement.esupsignature.url}")
	private String urlEsupsignature;
	
	@Value("${emargement.esupsignature.workflow.id}")
	private String workflowId;
	
	private final static String ITEM = "esupsignature";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@GetMapping(value = "/manager/esupsignature")
	public String index (@PathVariable String emargementContext, Model model, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable) {
		model.addAttribute("urlEsupSignature", urlEsupsignature);
		model.addAttribute("esupsignaturePage", esupSignatureRepository.findAll(pageable));
		return "manager/esupSignature/index";
	}
	
	@GetMapping(value = "/manager/esupsignature/sessionEpreuve/{id}")
	public String searchBySessionEpreuve (@PathVariable String emargementContext, @PathVariable  Long id, Model model, @PageableDefault(size = 20, direction = Direction.DESC) Pageable pageable) {
		model.addAttribute("urlEsupSignature", urlEsupsignature);
		SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
		model.addAttribute("esupsignaturePage", esupSignatureRepository.findBySessionEpreuve(se, pageable));
		return "manager/esupSignature/index";
	}


	//http://localhost:8080/Ctx-test/manager/esupsignature/status/12993
	@GetMapping(value = "/manager/esupsignature/status/{signId}")
	@Transactional
	public String getStatutPdf(@PathVariable String emargementContext, @PathVariable Long signId, 
			@RequestParam(required = false) String from, HttpServletResponse response) {
		
		RestTemplate restTemplate = new RestTemplate();
		String urlStatus = String.format("%s/ws/signrequests/status/%s", urlEsupsignature, signId);
		ResponseEntity<String> status = restTemplate.getForEntity(urlStatus, String.class);
		String statut = status.getBody();
		EsupSignature esupsignature = esupSignatureRepository.findBySignRequestId(signId).get(0);
		if(!esupsignature.getStatutSignature().name().equals(statut) && "completed".equals(statut)){
			//On charge le statut
			esupsignature.setStatutSignature(StatutSignature.valueOf(statut.toUpperCase()));
			esupsignature.setDateModification(new Date());
			esupSignatureRepository.save(esupsignature);
			//On récupère le PDF
			esupSignatureService.getLastPdf(emargementContext, esupsignature, signId);
		}else {
			esupsignature.setDateModification(new Date());
			esupSignatureRepository.save(esupsignature);
		}
		if(from!=null) {
			return String.format("redirect:/%s/manager/individu?eppnTagCheck=%s&idGroupe=&eppnTagChecker=", emargementContext, from);
		}
		return String.format("redirect:/%s/manager/esupsignature", emargementContext);
	}

	//http://localhost:8080/Ctx-test/manager/esupsignature/delete/293/12993
	@PostMapping(value = "/manager/esupsignature/delete/{id}")
	@Transactional
	public String deletePDF(@PathVariable String emargementContext, @PathVariable Long id) {
		EsupSignature esupsignature = esupSignatureRepository.findById(id).get();
		if(esupsignature != null) {
			Long storedFileId = esupsignature.getStoredFileId();
			if (storedFileId != null) {
				storedFileRepository.delete(storedFileRepository.findById(storedFileId).get());
			}
			esupSignatureRepository.delete(esupsignature);
		}
		return String.format("redirect:/%s/manager/esupsignature", emargementContext);
	}
}
