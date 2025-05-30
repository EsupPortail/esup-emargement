package org.esupportail.emargement.web.superadmin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;
import javax.validation.Valid;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.ContextService;
import org.esupportail.emargement.services.HelpService;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.UserAppService;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/{emargementContext}")
@PreAuthorize(value="@userAppService.isSuperAdmin()")
public class SuperAdminController {
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	ContextRepository contextRepository;

	@Autowired
	UserAppRepositoryCustom userAppRepositoryCustom;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	LdapService ldapService;
	
	@Resource
	ContextService contexteService;
	
	@Resource
	LogService logService;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	HelpService helpService;
	
	@Autowired
	ToolUtil toolUtil;
	
	private final static String ITEM = "admins";
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@ModelAttribute("active")
	public static String getActiveMenu() {
		return ITEM;
	}

	@GetMapping(value = "/superadmin/admins")
	public String list(@PathVariable String emargementContext, Model model,  @PageableDefault(size = 20, direction = Direction.ASC, sort = "eppn")  Pageable pageable, 
			@RequestParam(value="key", required = false) String key) throws InvalidNameException {
		
		Page<UserApp> userAppPage = null;
		List<String> contexts = contextRepository.findDistinctKey();
		if(key == null || key.equals("all")){
			key = "all";
			List<UserApp> articlesList = userAppService.getSuperAdmins(pageable);
			if (pageable.getOffset() >= articlesList.size()) {
				userAppPage = Page.empty();
			}else {
				int startIndex = (int)pageable.getOffset();
				int endIndex = (int) ((pageable.getOffset() + pageable.getPageSize()) > articlesList.size() ?
				articlesList.size() :
				pageable.getOffset() + pageable.getPageSize());
				List<UserApp> subList = articlesList.subList(startIndex, endIndex);
				userAppPage = new PageImpl<>(subList, pageable, articlesList.size());
			}
		}else {
			userAppPage = userAppRepository.findByUserRoleAndContextKey(Role.ADMIN, key, pageable);
		}
		userAppService.setNomPrenom(userAppPage.getContent(), true);
		model.addAttribute("currentKey", key);
		model.addAttribute("contexts", contexts);
        model.addAttribute("userAppPage", userAppPage);
		model.addAttribute("url", "/superadmin/admins");
		model.addAttribute("paramUrl", "0");
		model.addAttribute("help", helpService.getValueOfKey(ITEM));
		model.addAttribute("itsme", userAppService.getUserAppEppn());
		model.addAttribute("isAdminContext", userAppService.isAdminOfCurrentContext(emargementContext));
		return "superadmin/admins/list";
	}
	
	@GetMapping(value = "/superadmin/admins/{id}", produces = "text/html")
    public String show(@PathVariable("id") String id, Model uiModel) {
		
		UserApp userApp = null;
		List<UserApp> users = new ArrayList<>();
		if(toolUtil.isLong(id)) {
			users.add(userAppRepository.findById(Long.valueOf(id)).get());
		}else {
			users.add(userAppService.setSuperAdminUserApp(id));
		}
		userApp = userAppService.setNomPrenom(users, true).get(0);

        uiModel.addAttribute("userApp", userApp);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
        return "superadmin/admins/show";
    }
	
    @GetMapping(value = "/superadmin/admins", params = "form", produces = "text/html")
    public String createForm(@PathVariable String emargementContext, Model uiModel) {
    	UserApp userApp = new UserApp();
    	populateEditForm(uiModel, userApp, emargementContext);
        return "superadmin/admins/create";
    }
    
    @GetMapping(value = "/superadmin/admins/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable String emargementContext, @PathVariable("id") Long id, Model uiModel) {
    	UserApp userApp = userAppRepository.findById(id).get();
    	populateEditForm(uiModel, userApp, emargementContext);
        return "superadmin/admins/update";
    }
    
    void populateEditForm(Model uiModel, UserApp userApp, String context) {
    	uiModel.addAttribute("contexts", contextRepository.findAll());
    	uiModel.addAttribute("allRoles", userAppService.getAllRoles(context, userApp));
        uiModel.addAttribute("userApp", userApp);
        uiModel.addAttribute("help", helpService.getValueOfKey(ITEM));
    }
    
    @PostMapping("/superadmin/admins/create")
    public String create(@PathVariable String emargementContext, @Valid UserApp userApp, BindingResult bindingResult, Model uiModel, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, userApp, emargementContext);
            return "superadmin/admins/create";
        }
        uiModel.asMap().clear();
        userApp.setDateCreation(new Date());
        String contextKey = emargementContext;
        if(userApp.getContext()!=null) {
        	contextKey = userApp.getContext().getKey();
        }
        Context context = contextRepository.findByContextKey(contextKey);
        if(userAppRepository.countByEppnAndContext(userApp.getEppn(), context)>0) {
        	redirectAttributes.addFlashAttribute("user", userApp);
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la création, agent déjà existant : " + userApp.getEppn());
        	return String.format("redirect:/%s/superadmin/admins?form", emargementContext);
        }
    	userApp.setContext(context);
        userAppRepository.save(userApp);
        log.info("ajout agent : " + userApp.getEppn());
        logService.log(ACTION.AJOUT_AGENT, RETCODE.SUCCESS, "", userApp.getEppn(), null, emargementContext, null);
        redirectAttributes.addFlashAttribute("createOk", userApp);
        return String.format("redirect:/%s/superadmin/admins?key=%s", emargementContext, userApp.getContext().getKey());
    }
    
    @PostMapping("/superadmin/admins/update/{id}")
    public String update(@PathVariable String emargementContext, @PathVariable("id") Long id, @Valid UserApp userApp, BindingResult bindingResult, Model uiModel, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, userApp, emargementContext);
            return "superadmin/admins/update";
        }
        uiModel.asMap().clear();
        userApp.setDateCreation(new Date());
        String contextKey = emargementContext;
        if(userApp.getContext()!=null) {
        	contextKey = userApp.getContext().getKey();
        }
        Context context = contextRepository.findByContextKey(contextKey);
        UserApp oldUserApp = userAppRepository.findById(id).get();
        userApp.setId(id);
        if(userAppRepository.countByEppnAndContext(userApp.getEppn(), context)>0 && !oldUserApp.getContext().getKey().equals(userApp.getContext().getKey()) ) {
        	redirectAttributes.addFlashAttribute("eppn", userApp.getEppn());
        	redirectAttributes.addFlashAttribute("error", "constrainttError");
        	log.info("Erreur lors de la maj agent : " + userApp.getEppn());
        	logService.log(ACTION.UPDATE_AGENT, RETCODE.FAILED, "", userApp.getEppn(), null, emargementContext, null);
        	return String.format("redirect:/%s/superadmin/admins/%s?form", emargementContext, id);
        }
    	userApp.setUserRole(oldUserApp.getUserRole());
    	userApp.setContext(context);
    	userApp.setContextPriority(oldUserApp.getContextPriority());
    	userApp.setLastConnexion(oldUserApp.getLastConnexion());
        userAppRepository.save(userApp);
        log.info("Maj agent : " + userApp.getEppn());
        logService.log(ACTION.UPDATE_AGENT, RETCODE.SUCCESS, userApp.getUserRole().name().concat(" --> ").concat(userApp.getUserRole().name()), userApp.getEppn(), null, emargementContext, null);
        return String.format("redirect:/%s/superadmin/admins?key=%s", emargementContext, contextKey) ;
    }
    
    @PostMapping(value = "/superadmin/admins/{id}")
    public String delete(@PathVariable String emargementContext, @PathVariable("id") Long id, final RedirectAttributes redirectAttributes) {
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
    	return String.format("redirect:/%s/superadmin/admins?key=%s", emargementContext, userApp.getContext().getKey());
    }
    
    @GetMapping("/superadmin/search-users")
    public String searchUsers(@RequestParam(required = false)  String username, @RequestParam(required = false) String eppn, Model model) {
    	if(eppn!=null) {
    		username = eppn;
    	}
        if (username.length() < 3) {
            model.addAttribute("users", new ArrayList<>());
        } else {
            List<LdapUser> userAppsList = ldapService.search(username);
            model.addAttribute("users", userAppsList);
        }
        return "fragments/search-results :: searchResults";
    }
}
