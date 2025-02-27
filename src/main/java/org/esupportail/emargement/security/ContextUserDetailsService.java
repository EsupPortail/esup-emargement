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

import org.esupportail.emargement.config.EmargementConfig;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.LdapService;
import org.jasig.cas.client.validation.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class ContextUserDetailsService extends AbstractCasAssertionUserDetailsService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	UserAppRepository userAppRepository;
	
	ContextRepository contextRepository;
	
	LdapService ldapService;
	
	PersonRepository personRepository;
	
	EmargementConfig config;
	
	public ContextUserDetailsService(EmargementConfig config, UserAppRepository userAppRepository, ContextRepository contextRepository, LdapService ldapService, PersonRepository personRepository){
		this.config = config;
		this.userAppRepository = userAppRepository;
		this.contextRepository = contextRepository;
		this.ldapService = ldapService;
		this.personRepository = personRepository;
	}

	@Override
	protected UserDetails loadUserDetails(Assertion assertion) {

		String eppn = "";
		// si eduPersonPrincipalName proposé par CAS
		if(assertion.getPrincipal().getAttributes().get("eduPersonPrincipalName") != null) {
			eppn = assertion.getPrincipal().getAttributes().get("eduPersonPrincipalName").toString();
			log.info("Got eduPersonPrincipalName CAS attribute {} from  for cas assertion principal name {}", eppn, assertion.getPrincipal().getName());
		} else {
			// sinon récupération via ldap
			String uid = assertion.getPrincipal().getName();
			eppn = ldapService.getEppn(uid);
			log.info("No eduPersonPrincipalName Cas attribute from CAS for cas assertion principal name {} / got eppn from ldap : {}", uid, eppn);
		}

		Map<String, Set<GrantedAuthority>> contextAuthorities = new HashMap<>();
		List<String> availableContexts = new ArrayList<>(); 
		Map<String, Long> availableContextIds = new HashMap<>();
		Set<GrantedAuthority> rootAuthorities = new HashSet<>();

		Boolean isSuperAdmin = ldapService.checkIsUserInGroupSuperAdminLdap(eppn);
		if(isSuperAdmin) {
			rootAuthorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
			availableContexts.add("all");
			contextAuthorities.put("all", rootAuthorities);
		}

		List<Context> allcontexts = contextRepository.findAll();
		for(Context context: allcontexts) {
			if(context.getIsActif()) {
				String contextKey = context.getKey();
				Set<GrantedAuthority> authorities = new HashSet<>(rootAuthorities);
				Set<GrantedAuthority>  extraRoles = getEmargementAdditionalRoles(eppn, context);
				if(!extraRoles.isEmpty()) {
					authorities.addAll(extraRoles);
					if(!authorities.isEmpty()) {
						availableContexts.add(contextKey);
					}
					contextAuthorities.put(contextKey, authorities);
					Long id = null;
					id =contextRepository.findByContextKey(contextKey).getId();
					availableContextIds.put(contextKey, id);
				}
			}
		}

		// TODO : simplifier et factoriser le code avec UserDetailsServiceImpl.loadUserByUser
		//contexte par défaut en premier
		List<Object[]> list = contextRepository.findByEppn(eppn);
		HashMap<String,String> map = new HashMap<>();
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

		String displayName = ldapService.getUsers(eppn).get(0).getPrenomNom();
		ContextUserDetails contextUserDetails = new ContextUserDetails(eppn, displayName, contextAuthorities, availableContexts, availableContextIds);

		log.info("Authentication of {} - isSuperAdmin : {}, displayName : {}, contextAuthorities: {}",
				eppn, isSuperAdmin, displayName, contextAuthorities);

		return contextUserDetails;
	}
	
	protected Set<GrantedAuthority> getEmargementAdditionalRoles(String eppn, Context context) {

		Set<GrantedAuthority> extraRoles = new HashSet<>();
		
		UserApp userApp = userAppRepository.findByEppnAndContext(eppn, context);
		
		if(userApp!=null) {
			extraRoles.add(new SimpleGrantedAuthority("ROLE_".concat(userApp.getUserRole().name())));
		}
		List<Person> persons = personRepository.findByEppnAndContext(eppn, context);
		if(!persons.isEmpty()) {
			extraRoles.add(new SimpleGrantedAuthority("ROLE_USER"));
		}
		return extraRoles;
	}

}


