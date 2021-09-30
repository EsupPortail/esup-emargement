package org.esupportail.emargement.web.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.SearchBean;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.repositories.custom.TagCheckerRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.PersonService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.services.TagCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class Individucontroller {
	
	@Resource
	PersonService personService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	TagCheckerRepositoryCustom tagCheckerRepositoryCustom;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "individu";
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return  ITEM;
	}
	
	@GetMapping(value = "/manager/individu")
	public String list(Model model, @RequestParam(defaultValue = "", value="eppnTagCheck") String identifiantTagCheck, @RequestParam(defaultValue = "", 
	value="eppnTagChecker") String eppnTagChecker, @PageableDefault(direction = Direction.ASC,  size = 10)  Pageable p1){
		if(!identifiantTagCheck.isEmpty()) {
			Page<TagCheck> pTagChecks = null;
			if(tagCheckRepository.countTagCheckByPersonEppn(identifiantTagCheck)>0) {
				pTagChecks = tagCheckRepository.findTagCheckByPersonEppn(identifiantTagCheck, p1);
				tagCheckService.setNomPrenomTagChecks(pTagChecks.getContent());
			}else {
				pTagChecks = tagCheckRepository.findTagCheckByGuestEmail(identifiantTagCheck, p1);
			}
			model.addAttribute("tagChecksPage", pTagChecks);
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
	    		tagCheckService.setNomPrenomTagChecks(tagChecksList);
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
    	TreeSet<SearchBean> listWithoutDuplicates = searchBeans.stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SearchBean::getIdentifiant))));
        return listWithoutDuplicates;
    }
}
