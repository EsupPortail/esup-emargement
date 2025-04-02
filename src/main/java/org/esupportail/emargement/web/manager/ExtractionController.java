package org.esupportail.emargement.web.manager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.emargement.domain.ApogeeBean;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.services.ApogeeService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.GroupeService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.ImportExportService;
import org.esupportail.emargement.services.LdapGroupService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.TagCheckService;
import org.esupportail.emargement.services.UserAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.opencsv.CSVWriter;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin() or @userAppService.isManager()")
public class ExtractionController {

	public enum ExtractionType {apogee, ldap, csv, groupes}

	@Autowired(required = false)
	ApogeeService apogeeService;

	@Resource
	LogService logService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	GroupeService groupeService;
	
	@Resource
	UserAppService userAppService;

	@Resource
	LdapService ldapService;

	@Resource
	LdapGroupService ldapGroupService;
	
	@Resource
	ImportExportService importExportService;
	
	@Resource
	TagCheckService tagCheckService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Resource
	HelpService helpService;
	
	private final static String ITEM = "extraction";
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}

	@ModelAttribute("apogeeAvailable")
	public Boolean isApogeeAvailable() {
		return apogeeService != null;
	}
	
	@GetMapping(value = "/manager/extraction")
	public String index(Model uiModel, @RequestParam(required = false) ExtractionType type){
		uiModel.addAttribute("listTabs", appliConfigService.getListImportExport());
		return redirectTab(uiModel, importExportService.getDisplayType(type));
	}
	
	@GetMapping(value = "/manager/extraction/tabs/{type}", produces = "text/html")
    public String redirectTab(Model uiModel, @PathVariable ExtractionType type ) {
		uiModel.addAttribute("type", type.name());
		uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
		uiModel.addAttribute("allSessionEpreuves", importExportService.getNotFreeSessionEpreuve());
		if(ExtractionType.apogee.equals(type)) {
			uiModel.addAttribute("years", importExportService.getYearsUntilNow());
			if(apogeeService != null) {
				uiModel.addAttribute("allComposantes", apogeeService.getComposantes());
			} else {
				uiModel.addAttribute("allComposantes", new ArrayList<>());
			}
		}else if(ExtractionType.groupes.equals(type)) {
			uiModel.addAttribute("allGroupes", groupeService.getNotEmptyGroupes());
		}
		uiModel.addAttribute("listTabs", appliConfigService.getListImportExport());
		return "manager/extraction/index";
	}
	
	@PostMapping(value = "/manager/extraction/search")
	public void search(@PathVariable String emargementContext, ApogeeBean apogeebean, HttpServletResponse response){
		if(apogeeService == null) {
			log.warn("Apogée non configurée");
			return;
		}
		try {
			List<ApogeeBean> inscrits = apogeeService.getListeFutursInscrits(apogeebean);
			String filename = "users.csv";

			response.setContentType("text/csv");
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
			        "attachment; filename=\"" + filename + "\"");

			//create a csv writer
			CSVWriter writer = new CSVWriter(response.getWriter()); 
			
			for(ApogeeBean inscrit : inscrits) {
				List <String> line = new ArrayList<>();
				line.add(inscrit.getCodEtu());
				writer.writeNext(line.toArray(new String[1]));
			}
	        // closing writer connection 
	        writer.close(); 
			log.info("Extraction csv (apogee):  " + inscrits.size() + "résultats" );
			logService.log(ACTION.EXPORT_CSV, RETCODE.SUCCESS, "Extraction csv (apogee):" +  inscrits.size() + " résultats" , null,
							null, emargementContext, null);
		} catch (Exception e) {
			log.error("Erreur lors de l'extraction csv");
			logService.log(ACTION.EXPORT_CSV, RETCODE.FAILED, "Erreur lors de l'extraction csv (apogee)" , null, null, emargementContext, null);
			e.printStackTrace();
		}
	}
	
	@PostMapping(value = "/manager/extraction/csvFromLdap")
	public void csvFromLdap(@PathVariable String emargementContext, @RequestParam List<String> usersGroupLdap, HttpServletResponse response){
		try {
			if(!usersGroupLdap.isEmpty()) {
				String filename = "users.csv";
	
				response.setContentType("text/csv");
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				        "attachment; filename=\"" + filename + "\"");
	
				//create a csv writer
				CSVWriter writer = new CSVWriter(response.getWriter()); 
				
				for(String inscrit : usersGroupLdap) {
					List <String> line = new ArrayList<>();
					line.add(inscrit);
					writer.writeNext(line.toArray(new String[1]));
				}
		        // closing writer connection 
		        writer.close(); 
				log.info("Extraction csv (ldap):  " + usersGroupLdap.size() + "résultats" );
				logService.log(ACTION.EXPORT_CSV, RETCODE.SUCCESS, "Extraction csv (ldap):" +  usersGroupLdap.size() + " résultats" , null,
								null, emargementContext, null);
			}
		} catch (Exception e) {
			log.error("Erreur lors de l'extraction csv");
			logService.log(ACTION.EXPORT_CSV, RETCODE.FAILED, "Erreur lors de l'extraction csv (ldap)" , null, null, emargementContext, null);
			e.printStackTrace();
		}
	}
	
	@PostMapping(value = "/manager/extraction/csvFromGroupe")
	public void csvFromGroup(@PathVariable String emargementContext, @RequestParam(value = "groupe") List<Long> ids, HttpServletResponse response) {
		try {
			if(!ids.isEmpty()) {
				String filename = "users.csv";
	
				response.setContentType("text/csv");
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				        "attachment; filename=\"" + filename + "\"");
	
				//create a csv writer
				CSVWriter writer = new CSVWriter(response.getWriter()); 
				
				List<Person> allPersons =new ArrayList<>();
				for(Long id : ids) {
					Groupe groupe = groupeRepository.findById(id).get();
					allPersons.addAll(groupe.getPersons());
				}
				
				for(Person person : allPersons) {
					List <String> line = new ArrayList<>();
					line.add(person.getNumIdentifiant());
					writer.writeNext(line.toArray(new String[1]));
				}
		        // closing writer connection 
		        writer.close(); 
				log.info("Extraction csv (groupe) :  " + allPersons.size() + "résultats" );
				logService.log(ACTION.EXPORT_CSV, RETCODE.SUCCESS, "Extraction csv (groupe) :" +  allPersons.size() + " résultats" , null,
								null, emargementContext, null);
			}
		} catch (Exception e) {
			log.error("Erreur lors de l'extraction csv");
			logService.log(ACTION.EXPORT_CSV, RETCODE.FAILED, "Erreur lors de l'extraction csv (groupe)" , null, null, emargementContext, null);
			e.printStackTrace();
		}
	}
	
	@GetMapping("/manager/extraction/search/{param}")
    @ResponseBody
    public String searchDiplomes(@PathVariable String param, ApogeeBean apogeeBean) {
		List<ApogeeBean> apogeeBeans = new ArrayList<>();
		StringBuilder options = new StringBuilder("<option value=''>Select...</option>");
		if(apogeeService != null) {
			apogeeBeans = apogeeService.searchList(param, apogeeBean);
			for (ApogeeBean dip : apogeeBeans) {
				String value = "";
				String text = "";
				String detail = "";
				if("diplome".equals(param)) {
					text= dip.getLibEtp();
					value = dip.getCodEtp();
				}else if("matiere".equals(param)) {
					text= dip.getLibElp();
					value = dip.getCodElp();
					detail = " (" + value + ")";
				}else if("groupe".equals(param)) {
					text= dip.getLibGpe();
					value = dip.getCodExtGpe();
					detail = " (" + value + ")";
				}
		        options.append("<option value='").append(value).append("'>")
		               .append(text).append(detail).append("</option>");
		    }
		} else {
			log.warn("Apogée non configurée");
		}
		return options.toString();
    }
	
	@GetMapping("/manager/extraction/searchLocations")
    @ResponseBody
    public String searchLocations(@RequestParam(required = false) Long sessionEpreuve){
		StringBuilder options = new StringBuilder("<option value=''>-----Aucun----</option>");
		if(sessionEpreuve != null) {
			List<SessionLocation> locations = sessionLocationRepository.findSessionLocationBySessionEpreuveId(sessionEpreuve);
		    for (SessionLocation sl : locations) {
		        options.append("<option value='").append(sl.getId()).append("'>")
		               .append(sl.getLocation().getNom()).append(" (").append(sl.getCapacite()).append(")")
		               .append("</option>");
		    }
		}
		return options.toString();
    }
    
	@GetMapping("/manager/extraction/countAutorises/{param}")
    @ResponseBody
    public String countNbEtudiants(@PathVariable String param, ApogeeBean apogeeBean) {
		int nbEtudiants = apogeeService.countAutorises(param, apogeeBean);
		return "[" + nbEtudiants + "]";
    }
	
	@GetMapping("/manager/extraction/countItems/{param}")
    @ResponseBody
    public String countItems(@PathVariable String param, ApogeeBean apogeeBean) {
		int nbItems = apogeeService.searchList(param, apogeeBean).size();
		return "[" + nbItems + "]";
    }

    @GetMapping("/manager/extraction/ldap/searchGroup")
    @ResponseBody
    public  List<String> searchGroupsLdap(@RequestParam String searchValue) throws InvalidNameException {
    	 List<String> ldapGroups = ldapGroupService.getAllGroupNames(searchValue);
    	return ldapGroups;
    }
    
    @GetMapping("/manager/extraction/ldap/searchUsers")
    public String searchUsersLdap(Model uiModel, @RequestParam(value="searchString") String searchGroup) throws InvalidNameException {

		List<LdapUser> ldapMembers = ldapGroupService.getLdapMembers(searchGroup) ;
    	uiModel.addAttribute("group", searchGroup);
    	uiModel.addAttribute("ldapMembers", ldapMembers);
    	Statut statuts [] = {Statut.CLOSED, Statut.CANCELLED};
    	uiModel.addAttribute("allSessionEpreuves", sessionEpreuveRepository.findSessionEpreuveByStatutNotInOrderByDateExamen(Arrays.asList(statuts)));
		if(apogeeService != null) {
			uiModel.addAttribute("allComposantes", apogeeService.getComposantes());
		} else {
			uiModel.addAttribute("allComposantes", new ArrayList<>());
		}
		uiModel.addAttribute("years", importExportService.getYearsUntilNow());
		uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
		uiModel.addAttribute("size", helpService.getValueOfKey(ITEM));
		uiModel.addAttribute("type", "ldap");
		uiModel.addAttribute("listTabs", appliConfigService.getListImportExport());
    	return "manager/extraction/index";
    }
    
    @PostMapping(value = "/manager/extraction/importCsv", produces = "text/html")
    public String importCsv(@PathVariable String emargementContext, List<MultipartFile> files, @RequestParam(value="sessionEpreuve", required = false) Long id, 
    		@RequestParam(value= "sessionLocationCsv", required = false) Long slId, @RequestParam(required = false) String importTagchecker,
    		@RequestParam(required = false) String role, @RequestParam(required = false) String speciality,
    		final RedirectAttributes redirectAttributes) throws Exception {
    	List<InputStream> streams = new ArrayList<>();
    	for(MultipartFile file : files) {
    		streams.add(file.getInputStream());
    	}
    	SequenceInputStream is = new SequenceInputStream(Collections.enumeration(streams));
    	if(importTagchecker == null) {
	    	List<Integer> bilanCsv =  tagCheckService.importTagCheckCsv(new InputStreamReader(is), null, id, emargementContext, null, true, slId);
	    	redirectAttributes.addFlashAttribute("paramUrl", id);
	    	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
	    	redirectAttributes.addFlashAttribute("seLink", sessionEpreuveRepository.findById(id).get());
    	}else {
    		int nbImport = userAppService.importUserApp(userAppService.getEppnsFromCsv(is), contextRepository.findByContextKey(emargementContext) , role, speciality);
    		redirectAttributes.addFlashAttribute("bilanUserApp", nbImport);
    	}
    	return String.format("redirect:/%s/manager/extraction/tabs/csv", emargementContext);
    }
    
    @PostMapping("/manager/extraction/importFromApogee")
    @Transactional
    public String importFromApogee(@PathVariable String emargementContext, ApogeeBean apogeebean, Model uiModel,  
    		@RequestParam(value = "sessionLocation", required = false) Long slId, final RedirectAttributes redirectAttributes) throws Exception {
        uiModel.asMap().clear();
        List<ApogeeBean> futursInscrits = apogeeService.getListeFutursInscrits(apogeebean);
        List<List<String>> finalList = apogeeService.getListeFutursInscritsDirectImport(futursInscrits);
        Map<String,String> mapEtapes = apogeeService.getMapEtapes(apogeebean, futursInscrits);
        List<Integer> bilanCsv =  tagCheckService.importTagCheckCsv(null, finalList, apogeebean.getSessionEpreuve().getId(), emargementContext, mapEtapes, true, slId);
    	redirectAttributes.addFlashAttribute("paramUrl", apogeebean.getSessionEpreuve().getId());
    	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
    	redirectAttributes.addFlashAttribute("seLink", apogeebean.getSessionEpreuve());
    	return String.format("redirect:/%s/manager/extraction/tabs/apogee", emargementContext);
    }
    
    @PostMapping("/manager/extraction/importFromLdap")
    @Transactional
    public String importFromLdap(@PathVariable String emargementContext, @RequestParam(value="sessionEpreuve", required = false) Long id, @RequestParam List<String> usersGroupLdap, 
    		@RequestParam(value="sessionLocationLdap", required = false) Long slId, @RequestParam(required = false) String importTagchecker,
    		@RequestParam(required = false) String role, @RequestParam(required = false) String speciality, final RedirectAttributes redirectAttributes) throws Exception {
    	if(importTagchecker == null) {
    		List<List<String>> finalList = tagCheckService.getListForimport(usersGroupLdap);
        	List<Integer> bilanCsv =  tagCheckService.importTagCheckCsv(null, finalList, id, emargementContext, null, true, slId);
        	redirectAttributes.addFlashAttribute("paramUrl", id);
        	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
        	redirectAttributes.addFlashAttribute("seLink", sessionEpreuveRepository.findById(id).get());
    	}else {
    		int nbImport = userAppService.importUserApp(usersGroupLdap, contextRepository.findByContextKey(emargementContext) , role, speciality);
    		redirectAttributes.addFlashAttribute("bilanUserApp", nbImport);
    	}
    	return String.format("redirect:/%s/manager/extraction/tabs/ldap", emargementContext);
    }
    
    @PostMapping("/manager/extraction/importFromGroupe")
    @Transactional
    public String importFromGroupe(@PathVariable String emargementContext,  @RequestParam("sessionEpreuve") Long id, @RequestParam("groupe") List<Long> idGroupe,
    		@RequestParam(value="sessionLocationGroupe", required = false) Long slId, final RedirectAttributes redirectAttributes) throws Exception {
    	List<String> usersGroupe = groupeService.getUsersForImport(idGroupe);
    	List<List<String>> finalList = tagCheckService.getListForimport(usersGroupe);
    	List<Integer> bilanCsv =  tagCheckService.importTagCheckCsv(null, finalList, id, emargementContext, null, true, slId);
    	redirectAttributes.addFlashAttribute("paramUrl", id);
    	redirectAttributes.addFlashAttribute("bilanCsv", bilanCsv);
    	redirectAttributes.addFlashAttribute("seLink", sessionEpreuveRepository.findById(id).get());
    	return String.format("redirect:/%s/manager/extraction/tabs/groupes", emargementContext);
    }
}
