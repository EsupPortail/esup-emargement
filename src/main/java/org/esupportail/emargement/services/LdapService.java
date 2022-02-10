package org.esupportail.emargement.services;

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

import javax.naming.InvalidNameException;
import java.util.ArrayList;
import java.util.List;

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
		List<LdapUser> ldapUsers = getUsers(uid);
		if (ldapUsers != null && !ldapUsers.isEmpty()) {
			eppn = ldapUsers.get(0).getEppn();
		}
		return eppn;
	}

	public List<LdapUser> getUsers(String uid) {
		Iterable<LdapUser> users = ldapUserRepository.findAll(getUserLdapFilter(uid));
		return IterableUtils.toList(users);
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
		return !IterableUtils.isEmpty(isSuperAdmins);
    }
	
	public List<LdapUser> getUsers(String eppn, String uid) {
		List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
		String identifiant = (eppn != null)? eppn : uid;
		if(identifiant != null &&identifiant.startsWith(paramUtil.getGenericUser())) {
			String splitIdentifiant [] = identifiant.split("_");
			String prenom = StringUtils.capitalize(paramUtil.getGenericUser());
			String ctx = splitIdentifiant[1];
			LdapUser generic = new LdapUser();
			generic.setEppn(identifiant + "@" + nomDomaine);
			generic.setPrenomNom(StringUtils.capitalize(prenom) + " " + StringUtils.capitalize(ctx));
			ldapUsers.add(generic);
		} else {
			if(uid !=null) {
				ldapUsers = getUsers(uid);
			} else if(eppn != null) {
				ldapUsers = ldapUserRepository.findByEppnEquals(eppn);
			}
		}
		return ldapUsers;
	}
          
}
