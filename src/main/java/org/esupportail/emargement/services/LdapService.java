package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.naming.InvalidNameException;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.utils.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class LdapService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private LdapUserRepository ldapUserRepository;

	@Value("${ldap.userFilter}")
	private String userLdapFilter;

	@Value("${ldap.superAdminFilter}")
	private String superAdminLdapFilter;

	@Value("${app.nomDomaine}")
	private String nomDomaine;

	@Autowired
	ParamUtil paramUtil;

	LdapQuery getUserLdapFilter(String uid) {
		return LdapQueryBuilder.query().filter(String.format(userLdapFilter, uid));
	}

	LdapQuery getSuperAdminsLdapFilter() {
		return LdapQueryBuilder.query().filter(superAdminLdapFilter);
	}

	public List<LdapUser> search(final String search) {
		List<LdapUser> userList = ldapUserRepository.findByNomPrenomContainingIgnoreCase(search);
		if (userList == null) {
			userList = new ArrayList<LdapUser>();
		}
		return userList;
	}
	
	public List<Map<String,String>> searchEmails(String query) {
		List<LdapUser> userList = ldapUserRepository.findByEmailContainingIgnoreCase(query);
		List<String> emails = new ArrayList<String>();
		
		if (!userList.isEmpty()) {
			emails  = userList.stream()
                    .map(LdapUser::getEmail)
                    .collect(Collectors.toList());
		}
		Collections.sort(emails);
		List<Map<String,String>> temp = new ArrayList<Map<String,String>>();
		for(String mail : emails) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("label", mail);
			map.put("value", mail);
			temp.add(map);
		}
		return temp;
	}
	
	public String getEppn(String uid) {
		String eppn= uid + "@" + nomDomaine;
		Iterable<LdapUser> users = ldapUserRepository.findAll(getUserLdapFilter(uid));
		List<LdapUser> ldapUsers = IterableUtils.toList(users);
		if (ldapUsers != null && !ldapUsers.isEmpty()) {
			eppn = ldapUsers.get(0).getEppn();
		}
		log.info("getEppn({}) / {} -> {}", uid, String.format(userLdapFilter, uid), eppn);
		return eppn;
	}

    public List<LdapUser>  getAllSuperAdmins() throws InvalidNameException {
		String superAdminsLdapFilter = superAdminLdapFilter;
		Iterable<LdapUser> superAdmins = ldapUserRepository.findAll(LdapQueryBuilder.query().filter(superAdminsLdapFilter));
		return IterableUtils.toList(superAdmins);
    }

	public Boolean checkIsUserInGroupSuperAdminLdap(String eppn) {
		String superAdminsLdapFilter = superAdminLdapFilter;
		String isSuperAdminsLdapFilter = String.format("&(eduPersonPrincipalName=%s)(%s)", eppn, superAdminsLdapFilter);
		Iterable<LdapUser> isSuperAdmins = ldapUserRepository.findAll(LdapQueryBuilder.query().filter(isSuperAdminsLdapFilter));
		Boolean isSuperAdmin = !IterableUtils.isEmpty(isSuperAdmins);
		log.info("checkIsUserInGroupSuperAdminLdap({}) / {} -> {}", eppn, isSuperAdminsLdapFilter, isSuperAdmin);
		return isSuperAdmin;
    }
	
	public List<LdapUser> getUsers(String eppn) {
		List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
		if(eppn.startsWith(paramUtil.getGenericUser())) {
			String splitIdentifiant [] = eppn.split("_");
			String prenom = StringUtils.capitalize(paramUtil.getGenericUser());
			String ctx = splitIdentifiant[1];
			LdapUser generic = new LdapUser();
			generic.setEppn(eppn);
			generic.setPrenomNom(StringUtils.capitalize(prenom) + " " + StringUtils.capitalize(ctx));
			ldapUsers.add(generic);
		} else {
			ldapUsers = ldapUserRepository.findByEppnEquals(eppn);
		}
		return ldapUsers;
	}
	
	public String getPrenomNom(String eppn) {
		if(eppn!=null && !eppn.isEmpty()) {
			List<LdapUser> ldapUsers = ldapUserRepository.findByEppnEquals(eppn);
			if(!ldapUsers.isEmpty()) {
				return ldapUsers.get(0).getPrenomNom();
			}
			return eppn;
		}
		return eppn;
	}
	
	public Map<String, LdapUser> getLdapUsersFromNumList(List<String> numList, String filter) {
		if(!numList.isEmpty()) {
			LdapQuery queryBuilder = LdapQueryBuilder.query().where(filter).is(numList.get(0));
			for (int i = 1; i < numList.size(); i++) {
				queryBuilder = ((ContainerCriteria) queryBuilder).or(filter).is(numList.get(i));
			}
	
			Iterable<LdapUser> validators = ldapUserRepository.findAll(queryBuilder);
	
			if ("supannEtuId".equals(filter)) {
				return StreamSupport.stream(validators.spliterator(), false)
						.collect(Collectors.toMap(LdapUser::getNumEtudiant, Function.identity()));
			} else if ("eduPersonPrincipalName".equals(filter)) {
				return StreamSupport.stream(validators.spliterator(), false)
						.collect(Collectors.toMap(LdapUser::getEppn, Function.identity()));
			} else {
				return new HashMap<>();
			}
		}
		return new HashMap<>();
	}
}
