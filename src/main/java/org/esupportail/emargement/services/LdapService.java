package org.esupportail.emargement.services;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.utils.ParamUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;
import org.springframework.stereotype.Service;

@Service
public class LdapService {

	@Autowired
	private LdapUserRepository ldapUserRepository;
	
	@Value("${ldap.superAdminFilter}")
	private String superAdminLdapFilter;
	
	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	@Autowired
	ParamUtil paramUtil;

	public List<LdapUser> search(final String search) {
		List<LdapUser> userList = null;
		userList = ldapUserRepository.findByNomPrenomContainingIgnoreCase(search);

		if (userList == null) {
			return Collections.emptyList();
		}

		return userList;
	}
	
	public String getEppn(String uid) {
		String eppn = "";
		if( ldapUserRepository.findByUid(uid) != null && !ldapUserRepository.findByUid(uid).isEmpty()) {
			eppn = ldapUserRepository.findByUid(uid).get(0).getEppn();
		}else if(uid.startsWith(paramUtil.getGenericUser())) {
			eppn= uid + "@" + nomDomaine;
		}
		return eppn;
	}
    
    public List<LdapUser>  getAllSuperAdmins() throws InvalidNameException {
		String superAdminsLdapFilter = superAdminLdapFilter;
		Iterable<LdapUser> superAdmins = ldapUserRepository.findAll(LdapQueryBuilder.query().filter(superAdminsLdapFilter));
		return IterableUtils.toList(superAdmins);
    }

	public Boolean checkIsUserInGroupSuperAdminLdap(String uid) {
		String superAdminsLdapFilter = superAdminLdapFilter;
		String isSuperAdminsLdapFilter = String.format("&(uid=%s)(%s)", uid, superAdminsLdapFilter);
		Iterable<LdapUser> isSuperAdmins = ldapUserRepository.findAll(LdapQueryBuilder.query().filter(isSuperAdminsLdapFilter));
		return !IterableUtils.isEmpty(isSuperAdmins);
    }
	
	public List<LdapUser> getUserLdaps(String eppn, String uid) {
		List<LdapUser> ldapUsers = new ArrayList<LdapUser>();
		String identifiant = (eppn != null)? eppn : uid;
		if(identifiant != null &&identifiant.startsWith(paramUtil.getGenericUser())) {
			String splitIdentifiant [] = identifiant.split("_");
			String prenom = StringUtils.capitalize(paramUtil.getGenericUser());
			String ctx = splitIdentifiant[1];
			LdapUser generic = new LdapUser();
			generic.setUid(identifiant);
			generic.setEppn(identifiant + "@" + nomDomaine);
			generic.setPrenomNom(StringUtils.capitalize(prenom) + " " + StringUtils.capitalize(ctx));
			ldapUsers.add(generic);
		}else {
			if(uid !=null) {
				ldapUsers = ldapUserRepository.findByUid(uid);
			}else if(eppn != null) {
				ldapUsers = ldapUserRepository.findByEppnEquals(eppn);
			}
		}
		return ldapUsers;
	}
          
}
