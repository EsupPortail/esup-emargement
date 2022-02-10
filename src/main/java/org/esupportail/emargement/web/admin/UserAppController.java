package org.esupportail.emargement.web.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.UserAppService;
import org.esupportail.emargement.utils.ParamUtil;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isAdmin()")
public class UserAppController {
	
	@Autowired
	UserAppRepository userAppRepository;

	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired
	UserAppRepositoryCustom userAppRepositoryCustom;
	
	@Autowired
    LdapUserRepository ldapUserRepository;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	LogService logService;

	@Resource
	LdapService ldapService;
	
	@Resource
	HelpService helpService;

	@Resource
	AppliConfigService appliConfigService;
	
	@Autowired
	ParamUtil paramUtil;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String ITEM = "userApp";
	
	@Autowired
	ToolUtil toolUtil;
	
	@ModelAttribute("active")
	public String getActiveMenu() {
		return ITEM;
	}
	
	@GetMapping(value = "/admin/userApp")
	public String list(@PathVariable String emargementContext, Model model, @RequestParam(required = false, value="eppn") String eppn,  
			@PageableDefault(size = 1, direction = Direction.ASC, sort = "eppn")  Pageable pageable) {
		
		Long count = userAppRepository.count();
		
		int size = pageable.getPageSize();
		if( size == 1) {
			size = count.intValue();
		}
		
		Page<UserApp> userAppPage = userAppRepository.findAll(toolUtil.updatePageable(pageable, size));
		if(eppn != null) {
			Context context = contextRepository.findByContextKey(emargementContext);
			userAppPage = userAppRepository.findByEppnAndContext(eppn, context, pageable);
		}
		if(!userAppPage.isEmpty()) {
			userAppService.setNomPrenom(userAppPage.getContent(), true);
		}
		if(eppn!=null) {
			model.addAttribute("eppn", eppn);
			model.addAttribute("collapse", "show");
		}
		
        model.addAttribute("userAppPage", userAppPage);
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("itsme", userAppService.getUserAppEppn());
		model.addAttribute("isAdminContext", userAppService.isAdminOfCurrentContext(emargementContext));
		model.addAttribute("selectAll", count);
		return "admin/userApp/list";
	}
	
	@GetMapping(value = "/admin/userApp/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
		List<UserApp> users = new ArrayList<UserApp>();
		users.add(userAppRepository.findById(id).get());
		
        uiModel.addAttribute("userApp", userAppService.setNomPrenom(users, true).get(0));
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "admin/userApp/show";
    }
	
    @GetMapping(value = "/admin/userApp", params = "form", produces = "text/html")
    public String createForm(@PathVariable String emargementContext, Model uiModel) {
    	UserApp userApp = new UserApp();
    	populateEditForm(uiModel, userApp, emargementContext);
        return "admin/userApp/create";
    }
    
    @GetMapping(value = "/admin/userApp/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
    	UserApp userApp = userAppRepository.findById(id).get();
    	List<UserApp> userApps = new ArrayList<UserApp>();
    	userApps.add(userApp);
    	populateEditForm(uiModel, userAppService.setNomPrenom(userApps, true).get(0), emargementContext);
        return "admin/userApp/update";
    }
    
    void populateEditForm(Model uiModel, UserApp userApp, String context) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	List<LdapUser> ldapUser = ldapService.getUsers(auth.getName());
    	if(!ldapUser.isEmpty() && ldapUser.get(0).getEppn().startsWith(paramUtil.getGenericUser())) {
    		Context ctx = contextRepository.findByContextKey(context);
    		UserApp test =userAppRepository.findByEppnAndContext(ldapUser.get(0).getEppn(), ctx);
    		if(test == null) {
    			uiModel.addAttribute("ldapUser", ldapUser.get(0));
    		}
    	}
    	uiModel.addAttribute("contexts", userAppService.getUserContexts());
    	uiModel.addAttribute("allRoles", userAppService.getAllRoles(context, userApp));
        uiModel.addAttribute("userApp", userApp);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
    }
    
    @PostMapping("/admin/userApp/create")
    public String create(@PathVariable String emargementContext, @RequestParam(value="myEppn", required = false) String myEppn, @Valid UserApp userApp, BindingResult bindingResult, Model uiModel, 
    		HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || userApp.getEppn().isEmpty()&& myEppn == null) {
            populateEditForm(uiModel, userApp, emargementContext);
            return "admin/userApp/create";
        }
        uiModel.asMap().clear();
        userApp.setDateCreation(new Date());
        String contextKey = emargementContext;
        if(userApp.getContext()!=null) {
        	contextKey = userApp.getContext().getKey();
        }
        Context context = contextRepository.findByContextKey(contextKey);
        if(userAppRepository.countByEppnAndContext(userApp.getEppn(), context)>0) {
        	redirectAttributes.addFlashAttribute("eppn", userApp.getEppn());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, agent déjà existant : " + userApp.getEppn());
        	return String.format("redirect:/%s/admin/userApp?form", emargementContext);
        }else {
        	if(myEppn != null) {
        		userApp.setEppn(myEppn);
        	}
        	userApp.setContext(context);
            userAppRepository.save(userApp);
            log.info("ajout agent : " + userApp.getEppn());
            logService.log(ACTION.AJOUT_AGENT, RETCODE.SUCCESS, "", userApp.getEppn(), null, emargementContext, null);
            return String.format("redirect:/%s/admin/userApp", emargementContext);
        }
    }
    
    @PostMapping("/admin/userApp/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid UserApp userApp, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, userApp, emargementContext);
            return "admin/userApp/update";
        }
        uiModel.asMap().clear();
        userApp.setDateCreation(new Date());
        UserApp oldUserApp = userAppRepository.findById(id).get();
    	oldUserApp.setUserRole(userApp.getUserRole());
        userAppRepository.save(oldUserApp);
        log.info("Maj agent : " + userApp.getEppn());
        logService.log(ACTION.UPDATE_AGENT, RETCODE.SUCCESS, oldUserApp.getUserRole().name().concat(" --> ").concat(userApp.getUserRole().name()), userApp.getEppn(), null, emargementContext, null);
        return String.format("redirect:/%s/admin/userApp", emargementContext);
    }
    
    @PostMapping(value = "/admin/userApp/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel, final RedirectAttributes redirectAttributes) {
    	UserApp userApp = userAppRepository.findById(id).get();
    	try {
			userAppRepository.delete(userApp);
	    	log.info("Suppression agent : " + userApp.getEppn());
	    	logService.log(ACTION.DELETE_AGENT, RETCODE.SUCCESS, "", userApp.getEppn(), null, emargementContext, null);
		} catch (Exception e) {
			log.error("Erreur Suppression agent : " + userApp.getEppn(), e);
			logService.log(ACTION.DELETE_AGENT, RETCODE.FAILED, "", userApp.getEppn(), null, emargementContext, null);
			redirectAttributes.addFlashAttribute("item", userApp.getEppn());
			redirectAttributes.addFlashAttribute("error", "constrainttError");
		}

        return String.format("redirect:/%s/admin/userApp", emargementContext);
    }
    
    @GetMapping("/admin/userApp/searchUsersLdap")
    @ResponseBody
    public List<LdapUser> searchLdap(@RequestParam("searchValue") String searchValue) {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
    	List<LdapUser> userAppsList = new ArrayList<LdapUser>();
    	userAppsList = ldapService.search(searchValue);
    	
        return userAppsList;
    }
    
    @GetMapping("/admin/userApp/search")
    @ResponseBody
    public List<UserApp> search(@RequestParam("searchValue") String searchString){
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		List<UserApp> userApps= userAppRepositoryCustom.findAll(searchString);
		userAppService.setNomPrenom(userApps, true);
        return userApps;
    }
}
