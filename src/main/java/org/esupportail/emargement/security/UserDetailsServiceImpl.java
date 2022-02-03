package org.esupportail.emargement.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;

import org.esupportail.emargement.config.EmargementConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.services.LdapService;
import org.esupportail.emargement.services.LogService;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.services.UserAppService;
import org.esupportail.emargement.utils.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	
	@Autowired
	UserAppRepositoryCustom userAppRepositoryCustom;
	
	@Autowired
    LdapUserRepository ldapUserRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired	
	UserAppRepository userAppRepository;
	
	@Autowired
	PersonRepository personRepository;;
	
	@Autowired
	EmargementConfig config;
	
	@Resource
	LdapService ldapService;
	
	@Resource
	LogService logService;
	
	@Resource
	UserAppService userAppService;
	
	@Autowired
	ParamUtil paramUtil;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public UserDetails loadUserByUsername(String eppn) throws UsernameNotFoundException {
		
		List<LdapUser> ldapUsers = ldapService.getUserLdaps(eppn, null);
		
		if(!ldapUsers.isEmpty()) {
			LdapUser ldapUser = ldapUsers.get(0);
			return  loadUserByUser(ldapUser);
		}else {
			return null;
		}
	}	
	
	public UserDetails loadUserByUser(LdapUser targetUser){

		Map<String, Set<GrantedAuthority>> contextAuthorities = new HashMap<String, Set<GrantedAuthority>>();
		List<String> availableContexts = new ArrayList<String>(); 
		Map<String, Long> availableContextIds = new HashMap<String, Long>();
		Set<GrantedAuthority> rootAuthorities = new HashSet<GrantedAuthority>();
		
		try {
			boolean isSuperAdmin = ldapService.checkIsUserInGroupSuperAdminLdap(targetUser.getUid());
			if(isSuperAdmin) {
				rootAuthorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
				availableContexts.add("all");
				contextAuthorities.put("all", rootAuthorities);
			}
		} catch (InvalidNameException e) {
			log.error("Pb lors du test superdamin UserDetailsServiceImpl en mode su", e);
		}
		List<Context> allcontexts = new ArrayList<Context>();
		if(targetUser.getEppn().startsWith(paramUtil.getGenericUser())) {
			String ctxSplit [] = targetUser.getUid().split("_");
			Context ctx =contextRepository.findByContextKey(ctxSplit[1]);
			allcontexts.add(ctx);
		}else {
			 allcontexts = contextRepository.findAll();
		}
		
		for(Context context: allcontexts) {
			String contextKey = context.getKey();
			Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>(rootAuthorities);

			authorities.addAll(getEmargementAdditionalRoles(targetUser.getEppn(), context));
			
			if(!authorities.isEmpty()) {
				availableContexts.add(contextKey);
			}
			contextAuthorities.put(contextKey, authorities);
			Long id = null;
			id =contextRepository.findByContextKey(contextKey).getId();
			availableContextIds.put(contextKey, id);
		}
		//contexte par défaut en premier
		List<Object[]> list = contextRepository.findByEppn(targetUser.getEppn());
		HashMap<String,String> map = new HashMap<String, String>();
		for(Object[] o : list) {
			map.put(o[1].toString(), o[0].toString());
		}
		final Map<String, String> sortedByPriority = map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		Object [] ctxArray = sortedByPriority.keySet().toArray();
		
		if(ctxArray.length!=0) {
			String defaultContextUser = ctxArray[0].toString();
			int indexAll = availableContexts.indexOf("all");
			int index1 = availableContexts.indexOf(defaultContextUser);
			if(indexAll == -1) {
				if(index1 > 0) {
					Collections.swap(availableContexts, 0, index1);
				}
			}else if(indexAll == 0) {
				if(index1 > 1) {
					Collections.swap(availableContexts, 1, index1);
				}
			}
		}
		
		ContextUserDetails contextUserDetails = new ContextUserDetails(targetUser.getUid(), contextAuthorities, availableContexts, availableContextIds);
		logService.log(ACTION.SWITCH_USER, RETCODE.SUCCESS, "SU : " + targetUser.getEppn(), targetUser.getEppn(), null, "all", null);
		return contextUserDetails;

	}
	
	protected Set<GrantedAuthority> getEmargementAdditionalRoles(String eppn, Context context) {

		Set<GrantedAuthority> extraRoles = new HashSet<GrantedAuthority>();
		
		UserApp userApp = null;
		
		if(eppn.startsWith(paramUtil.getGenericUser())) {
			userApp = userAppService.setGenericUserApp(userApp, eppn, context);
		}else {
			userApp = userAppRepository.findByEppnAndContext(eppn, context);
		}
		
		if(userApp!=null) {
			extraRoles.add(new SimpleGrantedAuthority("ROLE_".concat(userApp.getUserRole().name())));
		}
		Person person = personRepository.findByEppnAndContext(eppn, context);
		if(person != null) {
			extraRoles.add(new SimpleGrantedAuthority("ROLE_USER"));
		}
		return extraRoles;
	}
}