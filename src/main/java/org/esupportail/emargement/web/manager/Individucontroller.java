package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.AppUser;
import org.esupportail.emargement.domain.AssiduiteBean;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.EsupSignature.StatutSignature;
import org.esupportail.emargement.domain.EsupSignature.TypeSignature;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.repositories.custom.TagCheckerRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.EsupSignatureService;
import org.esupportail.emargement.services.GroupeService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.services.TagCheckerService;
import org.esupportail.emargement.utils.ToolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;


@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class Individucontroller {
	
	@Resource
	PersonService personService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	EsupSignatureService esupSignatureService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	TagCheckerRepositoryCustom tagCheckerRepositoryCustom;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired
	EsupSignatureRepository esupSignatureRepository;
	
	@Autowired	
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource	
	GroupeService groupeService;
	
	@Resource
	HelpService helpService;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Value("${emargement.esupsignature.url}")
	private String urlEsupsignature;
	
	private final static String ITEM = "individu";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/manager/individu")
	public String list(@PathVariable String emargementContext, Model model,
			@RequestParam(required = false)  String typeSearch,
			@RequestParam(defaultValue = "")  String searchString,
			@RequestParam(required = false) String annee) {
		if(typeSearch!=null && "tagCheck".equals(typeSearch)) {
			Page<TagCheck> pTagChecks = null;
			if(tagCheckRepository.countTagCheckByPersonEppn(searchString)>0) {
				pTagChecks = tagCheckRepository.findTagCheckByPersonEppn(searchString, null);
				tagCheckService.setNomPrenomTagChecks(pTagChecks.getContent(), false, false);
			}else {
				pTagChecks = tagCheckRepository.findTagCheckByGuestEmail(searchString, null);
			}
			List<EsupSignature> signList = esupSignatureRepository.findByTagCheckIn(pTagChecks.getContent());
			Map<Long, EsupSignature> mapTc = new HashMap<>();
			if(!signList.isEmpty()) {
				for(EsupSignature sign : signList) {
					if(sign.getTagCheck() != null) {
						mapTc.put(sign.getTagCheck().getId(), sign);
					}
				}
			}
			List<TagCheck> tcs = pTagChecks.getContent();
			tagCheckService.setNbHoursSession(tcs);
			model.addAttribute("tagChecksPage", tcs);
			model.addAttribute("eppnTagCheck", searchString); 
			model.addAttribute("mapTc", mapTc);
			if(!pTagChecks.isEmpty()) {
				model.addAttribute("individu", tcs.get(0));	
			}
		}else if(typeSearch!=null && "tagChecker".equals(typeSearch)) {
			Page<TagChecker> pTagCheckers = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(searchString, null);
			tagCheckerService.setNomPrenom4TagCheckers(pTagCheckers.getContent());
			model.addAttribute("tagCheckersPage", pTagCheckers.getContent());
			if(!pTagCheckers.isEmpty()) {
				model.addAttribute("individu", pTagCheckers.getContent().get(0));	
			}
		}else if(typeSearch!=null && "groupe".equals(typeSearch)) {
			Long id = Long.valueOf(searchString);
			Page<AppUser> usersGroupePage = new PageImpl<>(groupeService.getMembers(id));
			model.addAttribute("usersGroupePage", usersGroupePage.getContent());
			Groupe groupe = groupeRepository.findById(id).get();
			if(!usersGroupePage.isEmpty()) {
				model.addAttribute("groupe", groupe);	
			}
			List<Groupe> groupes = new ArrayList<>();
			groupes.add(groupe);
			List<TagCheck> tcs = tagCheckRepository.findByPersonGroupesIn(groupes);
			Map<Person,List<TagCheck>> mapTcs =  null;
			if(annee != null ) {
				mapTcs = tcs.stream().filter(t -> t.getPerson() != null && annee.equals(t.getSessionEpreuve().getAnneeUniv()))
				        .collect(Collectors.groupingBy(t -> t.getPerson()));
			}else {
				mapTcs = tcs.stream().filter(t -> t.getPerson() != null)
				        .collect(Collectors.groupingBy(t -> t.getPerson()));
			}
			Map<Person, AssiduiteBean> mapAssiduite = new HashMap<>(); 
			//voir cas guest
			for (Person p : mapTcs.keySet()) {
		        List<AssiduiteBean> listAssiduite =  tagCheckService.setListAssiduiteBean(mapTcs.get(p), groupe.getAnneeUniv()) ;
		        mapAssiduite.put(p, listAssiduite.get(0));
		    }
			model.addAttribute("assiduiteMap", mapAssiduite);
			model.addAttribute("annee", annee);
			model.addAttribute("years", sessionEpreuveService.getYears(emargementContext));
		}else if(typeSearch!=null && "sessionEpreuve".equals(typeSearch)) {
			Long sessionEpreuveId = Long.valueOf(searchString);
			List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(sessionEpreuveId);
			tagCheckService.setNomPrenomTagChecks(tcs, false, false);
			model.addAttribute("activite", sessionEpreuveRepository.findById(sessionEpreuveId).get());	
			model.addAttribute("activitePage", tcs);
			model.addAttribute("nbBadgeage", tagCheckRepository.countBySessionEpreuveIdAndTagDateIsNotNullAndIsUnknownFalse(sessionEpreuveId));
		}
		model.addAttribute("types", personService.getTypesPerson());
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("isConvocationEnabled", appliConfigService.isConvocationEnabled());
		return "manager/individu/index";
	}
	
    @GetMapping("/manager/individu/attestation/{id}")
    public String getPDFPresence(@PathVariable String emargementContext, @PathVariable Long id,
    		 HttpServletResponse response){
    	
    	TagCheck tc = tagCheckRepository.findById(id).get();
		if(tc !=null) {
			Page <EsupSignature> page = esupSignatureRepository.findBySessionEpreuve(tc.getSessionEpreuve(), null);
			if(!page.isEmpty()) {
				for(EsupSignature esupsignature : page.getContent()) {
					RestTemplate restTemplate = new RestTemplate();
					String urlStatus = String.format("%s/ws/signrequests/status/%s", urlEsupsignature, id);
					ResponseEntity<String> status = restTemplate.getForEntity(urlStatus, String.class);
					String statut = status.getBody();
					if(esupsignature.getStatutSignature()!=null && !esupsignature.getStatutSignature().name().equals(statut) && "completed".equals(statut)){
						//On charge le statut
						esupsignature.setStatutSignature(StatutSignature.valueOf(statut.toUpperCase()));
						esupsignature.setDateModification(new Date());
						esupSignatureRepository.save(esupsignature);
						//On récupère le PDF
						esupSignatureService.getLastPdf(emargementContext, esupsignature, id);
					}else {
						esupsignature.setDateModification(new Date());
						esupSignatureRepository.save(esupsignature);
					}
				}
			}
		}
		String signRequestId = esupSignatureService.sendPdfToEsupToWorkflow(emargementContext, id, response, TypeSignature.INDIVIDUAL);

    	return String.format("redirect:%s/user/signrequests/%s", urlEsupsignature, signRequestId);
    }
    
    @GetMapping("/manager/individu/attestation/preview/{id}")
    public void previewPDFPresence(@PathVariable String emargementContext, @PathVariable Long id,
   		 HttpServletResponse response){
    	
    	tagCheckService.getAttestationPresence(id, response, emargementContext, false);
    }
    
    @GetMapping("/manager/individu/redirect/{id}")
    public String redirectEsupsignature(@PathVariable("id") Long signRequestId){
    	return String.format("redirect:%s/user/signrequests/%s", urlEsupsignature, signRequestId);
    }
}
