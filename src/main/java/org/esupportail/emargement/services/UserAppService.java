package org.esupportail.emargement.services;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ParamUtil;
import org.esupportail.emargement.web.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private UserAppRepositoryCustom userAppRepositoryCustom;

	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	LdapService ldapService;

	@Resource
	LogService logService;
	
	@Autowired
	ParamUtil paramUtil;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public List<Role> getAllRoles(String key){
		if("all".equals(key)) {
			return  Arrays.asList(Role.ADMIN);
		}
		Context context = contextRepository.findByContextKey(key);
		Role roleAuth = null;
		String eppn = getUserAppEppn();
		UserApp user = userAppRepository.findByEppnAndContext(eppn, context);
		if(user!=null) {
			roleAuth = user.getUserRole();
		}
		List<Role> newRoles = new ArrayList<>() ;
		
		if(Role.ADMIN.equals(roleAuth) || eppn.startsWith(paramUtil.getGenericUser())) {
			newRoles = Arrays.asList(new Role[] {Role.ADMIN, Role.MANAGER, Role.SUPERVISOR});
		}
		return newRoles;
	}

	public List<UserApp> getUserApps(List<TagChecker> tagCheckers) {

		List<UserApp> userApps = userAppRepository.findAll();
		List<UserApp> userAppsUsed = new ArrayList<>();

		if (!tagCheckers.isEmpty()) {
			for (TagChecker tagChecker : tagCheckers) {
				userAppsUsed.add(tagChecker.getUserApp());
			}
			userApps.removeAll(userAppsUsed);
		}
		setNomPrenom(userApps, true);
		return userApps;

	}
	
    public void setDateConnexion(String eppn) {
		List<UserApp> userApps =  userAppRepositoryCustom.findByEppn(eppn);
		if(!userApps.isEmpty()){
			 UserApp userApp = userApps.get(0);
			 userApp.setLastConnexion(new Date());
			 userAppRepository.save(userApp);
		 }
    }
    
	public List<UserApp> setNomPrenom(List<UserApp>allUserApps, boolean isIncluded){
		List<UserApp> newList = new ArrayList<>();
		if(!allUserApps.isEmpty()) {
			List<String> userAppList = allUserApps.stream().filter(userApp -> userApp.getEppn()!=null).map(userApp -> userApp.getEppn())
					.collect(Collectors.toList());
			Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(userAppList, "eduPersonPrincipalName");
			for(UserApp userApp : allUserApps) {
				LdapUser ldapUser = mapLdapUsers.get(userApp.getEppn());
				if(ldapUser!=null) {
					userApp.setNom(ldapUser.getName());
					userApp.setPrenom(ldapUser.getPrenom());
					newList.add(userApp);
				}
				if(ldapUser!=null && isIncluded) {
					newList.add(userApp);
				}
				if(ldapUser == null && isIncluded) {
					userApp.setNom("--");
					userApp.setPrenom("--");
					newList.add(userApp);
				}
				if(ldapUser == null && userApp.getEppn().startsWith(paramUtil.getGenericUser())) {
					userApp.setNom(userApp.getContext().getKey());
					userApp.setPrenom(StringUtils.capitalize(paramUtil.getGenericUser()));
					newList.add(userApp);
				}
			}
		}
		return newList;
	}
	
	public List<String> getUserContexts() {
		
		List<String> listContext = new ArrayList<>();
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
				eppn = auth.getName();
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

		Context allContext = new Context();
		allContext.setKey("ALL");
		List<LdapUser> ldapUsers =  ldapService.getAllSuperAdmins();
		List<UserApp> admins = new ArrayList<>();
		for(LdapUser ldapUser : ldapUsers) {
			UserApp admin = new UserApp();
			admin.setContext(allContext);
			admin.setUserRole(Role.SUPERADMIN);
			admin.setEppn(ldapUser.getEppn());
			admin.setNom(ldapUser.getName());
			admin.setPrenom(ldapUser.getPrenom());
			admins.add(admin);
		}
		String order = "eppn: ASC";
		if (pageable.getSort() != null) {
			order = pageable.getSort().toString();
		}
		if ("eppn: ASC".equals(order)) {
			admins.sort(Comparator.comparing(UserApp::getNom));
		} else {
			admins.sort(Comparator.comparing(UserApp::getNom).reversed());
		}
		return admins;
	}
	
	public UserApp setGenericUserApp(UserApp userApp, String eppn, Context context) {
		userApp = new UserApp();
		userApp.setEppn(eppn);
		userApp.setContext(context);
		userApp.setUserRole(UserApp.Role.ADMIN);
		userApp.setNom(context.getKey());
		userApp.setPrenom(StringUtils.capitalize(paramUtil.getGenericUser()));
		
		return userApp;
	}
	
	public UserApp setSuperAdminUserApp(String eppn) {
		UserApp userApp = new UserApp();
		Context context = new Context();
		context.setKey("ALL");
		userApp.setContext(context);
		userApp.setUserRole(Role.SUPERADMIN);
		userApp.setEppn(eppn);
		
		return userApp;
	}
	
	public int importUserApp(List<String> eppns, Context ctx, String role, String speciality) {
		Map<String, LdapUser> map =ldapService.getLdapUsersFromNumList(eppns,"eduPersonPrincipalName");
		List<UserApp> users =  userAppRepository.findByContext(ctx);
		int i = 0;
		for (Map.Entry<String, LdapUser> entry : map.entrySet()) {
			String eppn = entry.getValue().getEppn();
			LdapUser ldapUser = entry.getValue();
			if(!users.stream()
            .anyMatch(user -> eppn.equals(user.getEppn()))) {
				UserApp userApp = new UserApp();
				ldapUser.getEppn();
				userApp = new UserApp();
				userApp.setContext(ctx);
				userApp.setDateCreation(new Date());
				userApp.setContextPriority(0);
				userApp.setEppn(eppn);
				userApp.setSpeciality(speciality);
				userApp.setUserRole(role==null? Role.MANAGER : Role.SUPERVISOR);
				userAppRepository.save(userApp);
				i++;
			}
		}
		if(i>0) {
			logService.log(ACTION.AJOUT_AGENT, RETCODE.SUCCESS, "", "imports : " + i, null, ctx.getKey(), null);
		}
		return i;
	}
	
	public List<String> getEppnsFromCsv(SequenceInputStream sequenceInputStream) throws Exception {
		List<String> eppns = new ArrayList<>();
		try (Reader reader = new InputStreamReader(sequenceInputStream, StandardCharsets.UTF_8);
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
			for (CSVRecord record : csvParser) {
				String eppn = record.get(0);
	            if (eppn != null && !eppn.isEmpty()) {
	                eppns.add(eppn);
	            }
			}
		} catch (Exception e) {
			log.error("CSV d'import d'agents non conforme", e);
		}
		return eppns;
	}
}