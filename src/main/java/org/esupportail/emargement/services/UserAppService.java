package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserAppService {
	
	@Autowired
	private UserAppRepository userAppRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	UserLdapRepository userLdapRepository;

	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	private UserAppRepositoryCustom userAppRepositoryCustom;

	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	UserAppService userAppService;
	
	@Resource
	LdapService ldapService;
	
	private final String GENERIC_USER = "emargement";
	
	public String getGenericUser() {
		return GENERIC_USER;
	}

	public List<Role> getAllRoles(String key,  UserApp userApp){
		if("all".equals(key)) {
			return  Arrays.asList(Role.ADMIN);
		}else {
			Context context = contextRepository.findByContextKey(key);
			Role roleAuth = null;
			String eppn = getUserAppEppn();
			UserApp user = userAppRepository.findByEppnAndContext(eppn, context);
			if(user!=null) {
				roleAuth = user.getUserRole();
			}
			List<Role> newRoles = new ArrayList<Role>() ;
			
			if(Role.ADMIN.equals(roleAuth) || eppn.startsWith(GENERIC_USER)) {
				newRoles = Arrays.asList(new Role[] {Role.ADMIN, Role.MANAGER, Role.SUPERVISOR});
			}
			return newRoles;
		}
	}

	public List<UserApp> getUserApps(List<TagChecker> tagCheckers) {

		List<UserApp> userApps = userAppRepository.findAll();
		List<UserApp> userAppsUsed = new ArrayList<UserApp>();

		if (!tagCheckers.isEmpty()) {
			for (TagChecker tagChecker : tagCheckers) {
				userAppsUsed.add(tagChecker.getUserApp());
			}
			userApps.removeAll(userAppsUsed);
		}
		setNomPrenom(userApps, true);
		return userApps;

	}
	
    public void setDateConnexion(String userName) {
		 List<UserLdap> userLdaps =  userLdapRepository.findByUid(userName);
		 if(!userLdaps.isEmpty()) {
			 List<UserApp> userApps =  userAppRepositoryCustom.findByEppn(userLdaps.get(0).getEppn());
			 if(!userApps.isEmpty()){
				 UserApp userApp = userApps.get(0);
				 userApp.setLastConnexion(new Date());
				 userAppRepository.save(userApp);
			 }
		 }
    }
    
	public List<UserApp> setNomPrenom(List<UserApp>allUserApps, boolean isIncluded){
		List<UserApp> newList = new ArrayList<UserApp>();
		if(!allUserApps.isEmpty()) {
			for(UserApp userApp : allUserApps) {
				List<UserLdap> userLdap = userLdapRepository.findByEppnEquals(userApp.getEppn());
				if(!userLdap.isEmpty()) {
					userApp.setNom(userLdap.get(0).getUsername());
					userApp.setPrenom(userLdap.get(0).getPrenom());
					newList.add(userApp);
				}
				if(!userLdap.isEmpty() && isIncluded) {
					newList.add(userApp);
				}
				if(userLdap.isEmpty() && isIncluded) {
					userApp.setNom("--");
					userApp.setPrenom("--");
					newList.add(userApp);
				}
				if(userLdap.isEmpty() && userApp.getEppn().startsWith(userAppService.getGenericUser())) {
					userApp.setNom(userApp.getContext().getKey());
					userApp.setPrenom(StringUtils.capitalize(GENERIC_USER));
					newList.add(userApp);
				}
			}
		}
		return newList;
	}
	
	public List<String> getUserContexts() {
		
		List<String> listContext = new ArrayList<String>();
		if(!WebUtils.availableContexts().isEmpty()) {
			for(String ctx : WebUtils.availableContexts()) {
				if(!"all".equals(ctx)) {
					listContext.add(ctx);
				}
			}
		}
		return listContext;
	}
	
	public boolean isAdminOfCurrentContext(String key){
		
		boolean isAdmin = false;
		if(!"all".equals(key)) {
			Context context = contextRepository.findByContextKey(key);
			String eppn =  getUserAppEppn();
			UserApp user = userAppRepository.findByEppnAndContext(eppn, context);
			if(user!=null) {
				Role roleAuth = user.getUserRole();
				if(Role.ADMIN.equals(roleAuth)) {
					isAdmin = true;
				}
			}
		}else {
			isAdmin = true;
		}
			
		return isAdmin;
	}
	
	public String getUserAppEppn() {
		String eppn = "";
		
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if(auth != null
				&& auth.getPrincipal() != null
				&& auth.getPrincipal() instanceof UserDetails) {
			if(auth.getPrincipal()!=null) {
				eppn = ldapService.getEppn(auth.getName());
			}
		}
		return eppn;
	}
	
	public List<UserApp> allUserApps(){
		List<UserApp>  list = userAppRepository.findAll();
		list = setNomPrenom(list, true);
		Set<String> set = new HashSet<>(list.size());
		list.removeIf(p -> !set.add(p.getEppn()));
		list.removeIf(obj -> obj.getEppn().equals(getUserAppEppn()));
		return list;
	}
	
	public boolean isSuperAdmin() {
		return WebUtils.isSuperAdmin();
	}
	
	public boolean isAdmin() {
		return WebUtils.isAdmin();
	}
	
	public boolean isManager() {
		return WebUtils.isManager();
	}
	
	public boolean isSupervisor() {
		return WebUtils.isSupervisor();
	}
	
	public boolean isUser() {
		return WebUtils.isUser();
	}
	
	public List<UserApp> getSuperAdmins(Pageable pageable) throws InvalidNameException{
		
		List<Map<String, List<String>>> allMembers = ldapService.getAllmembers("", true);
		List<UserApp> users = new ArrayList<UserApp>();
		for(Map<String, List<String>> map : allMembers) {
			UserApp userApp = new UserApp();
			Context context = new Context();
			context.setKey("ALL");
			userApp.setContext(context);
			userApp.setUserRole(Role.SUPERADMIN);
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				if("eduPersonPrincipalName".equals((entry.getKey()))){
					userApp.setEppn(entry.getValue().get(0));
				}else if("sn".equals((entry.getKey()))){
					userApp.setNom(entry.getValue().get(0));
				}else if("givenName".equals((entry.getKey()))){
					userApp.setPrenom(entry.getValue().get(0));
				}
			}
			users.add(userApp);
			String order = "eppn: ASC";
			if(pageable.getSort()!=null){
		        order= pageable.getSort().toString();
			}
			if("eppn: ASC".equals(order)) {
				users.sort(Comparator.comparing(UserApp::getNom));
			}else {
				users.sort(Comparator.comparing(UserApp::getNom).reversed());
			}
		}
		return users;
	}
	
	public UserApp setGenericUserApp(UserApp userApp, String eppn, Context context) {
		userApp = new UserApp();
		userApp.setEppn(eppn);
		userApp.setContext(context);
		userApp.setUserRole(UserApp.Role.ADMIN);
		userApp.setNom(context.getKey());
		userApp.setPrenom(StringUtils.capitalize(GENERIC_USER));
		
		return userApp;
	}

}