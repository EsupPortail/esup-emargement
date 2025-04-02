package org.esupportail.emargement.web.supervisor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;

import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SearchBean;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.custom.LocationRepositoryCustom;
import org.esupportail.emargement.repositories.custom.SessionEpreuveRepositoryCustom;
import org.esupportail.emargement.repositories.custom.TagCheckRepositoryCustom;
import org.esupportail.emargement.repositories.custom.TagCheckerRepositoryCustom;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.services.LdapGroupService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.services.TagCheckerService;
import org.esupportail.emargement.services.UserAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager() or @userAppService.isSupervisor()")
public class SearchController {
	
	@Autowired
	UserAppRepositoryCustom userAppRepositoryCustom;
	
	@Autowired
	LocationRepositoryCustom locationRepositoryCustom;
	
	@Autowired
	SessionEpreuveRepositoryCustom sessionEpreuveRepositoryCustom;
	
	@Autowired
	TagCheckRepositoryCustom tagCheckRepositoryCustom;
	
	@Autowired
	TagCheckerRepositoryCustom tagCheckerRepositoryCustom;
	
	@Autowired	
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	TagCheckerService tagCheckerService;
	
	@Resource
	LdapGroupService ldapGroupService;
	
	@Resource
	LdapService ldapService;
	
    @GetMapping("/supervisor/search/{type}")
    public String searchUsers(@PathVariable String emargementContext, @PathVariable String type, 
    		@RequestParam String searchString, @RequestParam(required = false) Long seId, Model model) throws InvalidNameException {
        if (searchString.length() > 3) {
        	if("userApp".equals(type)) {
            	List<UserApp> userApps= userAppRepositoryCustom.findAll(searchString);
        		userAppService.setNomPrenom(userApps, true);
        		model.addAttribute("userApps", userApps);
        	}else if("location".equals(type)) {
        		List<Location> locations= locationRepositoryCustom.findAll(searchString, emargementContext);
        		model.addAttribute("locations", locations);
        	}else if("sessionEpreuve".equals(type)) {
        		List<SessionEpreuve> sessionEpreuves= sessionEpreuveRepositoryCustom.findAll(searchString);
        		model.addAttribute("sessionEpreuves", sessionEpreuves);
        	}else if("individuTagCheck".equals(type)) {
        		List<SearchBean> searchBeans = new ArrayList<>();
            	List<TagCheck>  tagChecksList = tagCheckRepositoryCustom.findAll(searchString, null);
            	List<TagCheck>  tagChecksList2 = tagCheckRepositoryCustom.findAll2(searchString, null);
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
    	    	TreeSet<SearchBean> listWithoutDuplicates = searchBeans.stream()
    	                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SearchBean::getIdentifiant))));
        		model.addAttribute("individus", listWithoutDuplicates);
        	}else if("individuTagChecker".equals(type)) {
        		List<SearchBean> searchBeans = new ArrayList<>();
        		List<TagChecker>  tagCheckersList = tagCheckerRepositoryCustom.findAll(searchString, null);
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
    	    	TreeSet<SearchBean> listWithoutDuplicates = searchBeans.stream()
    	                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SearchBean::getIdentifiant))));
        		model.addAttribute("individus", listWithoutDuplicates);
        	}else if("individuSessionEpreuve".equals(type)) {
        		List<SearchBean> searchBeans = new ArrayList<>();
        		List<SessionEpreuve> ses = sessionEpreuveRepository.findByNomSessionEpreuveLikeIgnoreCase("%" + searchString + "%");
        		for(SessionEpreuve se : ses) {
        			SearchBean searchBean = new SearchBean();
        			searchBean.setIdentifiant(se.getId().toString());
        			searchBean.setSessionEpreuve(se);
        			searchBeans.add(searchBean);
        		}
        		TreeSet<SearchBean> listWithoutDuplicates = searchBeans.stream()
    	                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SearchBean::getIdentifiant))));
        		model.addAttribute("searchBeans", listWithoutDuplicates);
        	}else if("individuGroupe".equals(type)) {
        		List<SearchBean> searchBeans = new ArrayList<>();
        		List<Groupe> groupes = groupeRepository.findByNomLikeIgnoreCase("%" + searchString + "%");
        		for(Groupe groupe : groupes) {
        			SearchBean searchBean = new SearchBean();
        			searchBean.setIdentifiant(groupe.getNom());
        			searchBean.setGroupe(groupe);
        			searchBeans.add(searchBean);
        		}
        		TreeSet<SearchBean> listWithoutDuplicates = searchBeans.stream()
                        .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SearchBean::getIdentifiant))));
        		model.addAttribute("searchBeans", listWithoutDuplicates);
        	}else if("ldapGroups".equals(type)) {
        		List<String> ldapGroups = ldapGroupService.getAllGroupNames(searchString);
        		model.addAttribute("ldapGroups", ldapGroups);
        	}else if("ldap".equals(type)) {
                List<LdapUser> userAppsList = ldapService.search(searchString);
                model.addAttribute("users", userAppsList);
            }else if("tagCheck".equals(type)) {
            	List<TagCheck>  tagChecks = tagCheckRepositoryCustom.findAll(searchString, seId);
            	if(!tagChecks.isEmpty()) {
            		tagCheckService.setNomPrenomTagChecks(tagChecks, false, false);
            	}
            	model.addAttribute("tagChecks", tagChecks);
            }
        	model.addAttribute("type", type);
        }
        return "fragments/search-results :: searchResults";
    }

}
