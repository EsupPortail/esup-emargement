package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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
import org.esupportail.emargement.domain.SearchBean;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource	
	GroupeService groupeService;
	
	@Resource
	HelpService helpService;
	
	@Value("${emargement.esupsignature.url}")
	private String urlEsupsignature;
	
	private final static String ITEM = "individu";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/manager/individu")
	public String list(@PathVariable String emargementContext, Model model, @RequestParam(defaultValue = "", value="eppnTagCheck") String identifiantTagCheck, @RequestParam(defaultValue = "", 
	value="eppnTagChecker") String eppnTagChecker, @RequestParam(defaultValue = "", value="idGroupe") String idGroupe, 
			@RequestParam(value="annee", required = false) String annee, @PageableDefault(direction = Direction.ASC,  size = 10)  Pageable p1, HttpServletResponse response){
		if(!identifiantTagCheck.isEmpty()) {
			Page<TagCheck> pTagChecks = null;
			if(tagCheckRepository.countTagCheckByPersonEppn(identifiantTagCheck)>0) {
				pTagChecks = tagCheckRepository.findTagCheckByPersonEppn(identifiantTagCheck, p1);
				tagCheckService.setNomPrenomTagChecks(pTagChecks.getContent(), false, false);
			}else {
				pTagChecks = tagCheckRepository.findTagCheckByGuestEmail(identifiantTagCheck, p1);
			}
			List<EsupSignature> signList = esupSignatureRepository.findByTagCheckIn(pTagChecks.getContent());
			Map<Long, EsupSignature> mapTc = new HashMap();
			if(!signList.isEmpty()) {
				for(EsupSignature sign : signList) {
					if(sign.getTagCheck() != null) {
						mapTc.put(sign.getTagCheck().getId(), sign);
					}
				}
			}
			model.addAttribute("assiduiteList",tagCheckService.setListAssiduiteBean(pTagChecks.getContent()));
			model.addAttribute("tagChecksPage", pTagChecks);
			model.addAttribute("mapTc", mapTc);
			if(!pTagChecks.isEmpty()) {
				model.addAttribute("individu", pTagChecks.getContent().get(0));	
			}
		}else if(!eppnTagChecker.isEmpty()) {
			Page<TagChecker> pTagCheckers = tagCheckerRepository.findTagCheckerByUserAppEppnEquals(eppnTagChecker, p1);
			tagCheckerService.setNomPrenom4TagCheckers(pTagCheckers.getContent());
			model.addAttribute("tagCheckersPage", pTagCheckers);
			if(!pTagCheckers.isEmpty()) {
				model.addAttribute("individu", pTagCheckers.getContent().get(0));	
			}			
		}
		else if(!idGroupe.isEmpty()) {
			//Annee
			
			Long id = Long.valueOf(idGroupe);
			Page<AppUser> usersGroupePage = new PageImpl<>(groupeService.getMembers(id));
			List<Long> test = usersGroupePage.getContent().stream().filter(t -> t.getPersonOrGuestId() != null)
					.map(t -> t.getPersonOrGuestId()).collect(Collectors.toList());
			model.addAttribute("usersGroupePage", usersGroupePage);
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
			Map<Person, AssiduiteBean> mapAssiduite = new HashMap(); 
			//voir cas guest
			for (Person p : mapTcs.keySet()) {
		        List<AssiduiteBean> listAssiduite =  tagCheckService.setListAssiduiteBean(mapTcs.get(p)) ;
		        mapAssiduite.put(p, listAssiduite.get(0));
		    }
			model.addAttribute("assiduiteMap", mapAssiduite);
			model.addAttribute("annee", annee);
			model.addAttribute("years", sessionEpreuveService.getYears(emargementContext));
		}
		
		model.addAttribute("types", personService.getTypesPerson());
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("isConvocationEnabled", appliConfigService.isConvocationEnabled());
		return "manager/individu/index";
	}
	
    @GetMapping("/manager/individu/search")
    @ResponseBody
    public TreeSet<SearchBean> searchLdap(@RequestParam("searchValue") String searchValue, @RequestParam("type") String type) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	List<SearchBean> searchBeans = new ArrayList<SearchBean>();
    	List<TagCheck>  tagChecksList = tagCheckRepositoryCustom.findAll(searchValue, null);
    	List<TagCheck>  tagChecksList2 = tagCheckRepositoryCustom.findAll2(searchValue, null);
    	if("tagCheck".equals(type)) {
	    	if(!tagChecksList.isEmpty()) {
	    		tagCheckService.setNomPrenomTagChecks(tagChecksList, false, false);
	    		for(TagCheck tc : tagChecksList) {
	    			SearchBean searchBean = new SearchBean();
	    			searchBean.setNom(tc.getPerson().getNom());
	    			searchBean.setPrenom(tc.getPerson().getPrenom());
	    			searchBean.setTypeObject("interne");
	    			searchBean.setIdentifiant(tc.getPerson().getEppn());
	    			searchBean.setId(tc.getId());
	    			searchBeans.add(searchBean);
	    		}
	    		for(TagCheck tc : tagChecksList2) {
	    			SearchBean searchBean = new SearchBean();
	    			searchBean.setNom(tc.getGuest().getNom());
	    			searchBean.setPrenom(tc.getGuest().getPrenom());
	    			searchBean.setTypeObject("externe");
	    			searchBean.setIdentifiant(tc.getGuest().getEmail());
	    			searchBean.setId(tc.getId());
	    			searchBeans.add(searchBean);	
	    		}
	    	}
	    	tagChecksList.addAll(tagChecksList2);
    	}
    	if("tagChecker".equals(type)) {
    		List<TagChecker>  tagCheckersList = tagCheckerRepositoryCustom.findAll(searchValue, null);
	    	if(!tagCheckersList.isEmpty()) {
	    		tagCheckerService.setNomPrenom4TagCheckers(tagCheckersList);
	    		for(TagChecker tc : tagCheckersList) {
	    			SearchBean searchBean = new SearchBean();
	    			searchBean.setNom(tc.getUserApp().getNom());
	    			searchBean.setPrenom(tc.getUserApp().getPrenom());
	    			searchBean.setIdentifiant(tc.getUserApp().getEppn());
	    			searchBean.setTypeObject("surveillant");
	    			searchBean.setId(tc.getId());
	    			searchBeans.add(searchBean);
	    		}
	    	}
    	}
    	if("groupe".equals(type)) {
    		List<Groupe> groupes = groupeRepository.findByNomLike("%" + searchValue + "%");
    		for(Groupe groupe : groupes) {
    			SearchBean searchBean = new SearchBean();
    			searchBean.setIdentifiant(groupe.getNom());
    			searchBean.setGroupe(groupe);
    			searchBeans.add(searchBean);
    		}
    	}
    	TreeSet<SearchBean> listWithoutDuplicates = searchBeans.stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SearchBean::getIdentifiant))));
        return listWithoutDuplicates;
    }
    
    @GetMapping("/manager/individu/attestation/{id}")
    public String getPDFPresence(@PathVariable String emargementContext, @PathVariable("id") Long id,
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
					if(!esupsignature.getStatutSignature().name().equals(statut) && "completed".equals(statut)){
						//On charge le statut
						esupsignature.setStatutSignature(StatutSignature.valueOf(statut.toUpperCase()));
						esupsignature.setDateModification(new Date());
						esupSignatureRepository.save(esupsignature);
						//On récupère le PDF
						esupSignatureService.getLastPdf(emargementContext, esupsignature, id, response);
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
    public void previewPDFPresence(@PathVariable String emargementContext, @PathVariable("id") Long id,
   		 HttpServletResponse response){
    	
    	tagCheckService.getAttestationPresence(id, response, emargementContext, false);
    }
    
    @GetMapping("/manager/individu/redirect/{id}")
    public String redirectEsupsignature(@PathVariable String emargementContext, @PathVariable("id") Long signRequestId,
    		HttpServletResponse response){

    	return String.format("redirect:%s/user/signrequests/%s", urlEsupsignature, signRequestId);
    }
}
