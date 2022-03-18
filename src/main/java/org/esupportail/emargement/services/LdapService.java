package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;

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
			}else {
				return eppn;
			}
		}else {
			return eppn;
		}
	}
}
