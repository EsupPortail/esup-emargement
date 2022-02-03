package org.esupportail.emargement.services;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.UserLdapRepository;
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
	private UserLdapRepository userLdapRepository;
	
	private static final Integer THREE_SECONDS = 3000;
	
	@Autowired
    private LdapTemplate ldapTemplate;
	
	@Value("${ldap.groups}")
	private String ldapGroups;
	
	@Value("${emargement.ruleSuperAdmin.memberOf}")
	private String ruleSuperAdmin;
	
	@Value("${emargement.ruleSuperAdmin.uid}")
	private String ruleSuperAdminUid;
	
	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	@Autowired
	ParamUtil paramUtil;

	public List<UserLdap> search(final String search) {
		List<UserLdap> userList = null;
		userList = userLdapRepository.findByNomPrenomContainingIgnoreCase(search);

		if (userList == null) {
			return Collections.emptyList();
		}

		return userList;
	}
	
	public String getEppn(String uid) {
		String eppn = "";
		if( userLdapRepository.findByUid(uid) != null && !userLdapRepository.findByUid(uid).isEmpty()) {
			eppn = userLdapRepository.findByUid(uid).get(0).getEppn();
		}else if(uid.startsWith(paramUtil.getGenericUser())) {
			eppn= uid + "@" + nomDomaine;
		}
		return eppn;
	}
    
    public List<String> getAllGroupNames(String searchValue) throws InvalidNameException {
		LdapName ldapBase = new LdapName(ldapGroups);
		String searchfilter = (searchValue.isEmpty())? "*": "*".concat(searchValue).concat("*");
		List<String>  groups = ldapTemplate.search(
				query().base(ldapBase).searchScope(SearchScope.SUBTREE).timeLimit(THREE_SECONDS).where("cn").like(searchfilter),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs) throws NamingException {
						return attrs.get("cn").get().toString();
					}
				});
		return groups;
    }
    
    public List<UserLdap>  getAllmembers(String searchValue) throws InvalidNameException {
    	String searchUsers = "";
    	if(!searchValue.trim().isEmpty()) {
    		searchUsers = "(memberOf=cn=" + searchValue + "," + ldapGroups+")";
    	}
		return IterableUtils.toList(userLdapRepository.findAll(LdapQueryBuilder.query().filter(searchUsers)));
    }
    
    public List<UserLdap>  getAllSuperAdmins() throws InvalidNameException {
		String superAdminsLdapFilter = getSuperAdminsLdapFilter();
		Iterable<UserLdap> superAdmins = userLdapRepository.findAll(LdapQueryBuilder.query().filter(superAdminsLdapFilter));
		return IterableUtils.toList(superAdmins);
    }

	private String getSuperAdminsLdapFilter() {
		String superAdminsLdapFilter = "";
		if(!ruleSuperAdminUid.trim().isEmpty()) {
			String splitUids []= ruleSuperAdminUid.split(",");
			StringBuilder res = new StringBuilder();
			for(int i=0; i<splitUids.length; i++) {
				res.append("(uid=" + splitUids [i].trim() + ")");
			}
			superAdminsLdapFilter = "(|"+ res.toString() + ")";
		}
		else {
			superAdminsLdapFilter = "memberOf=" + ruleSuperAdmin;
		}
		return superAdminsLdapFilter;
	}


	public Boolean checkIsUserInGroupSuperAdminLdap(String uid) throws InvalidNameException {
		String superAdminsLdapFilter = getSuperAdminsLdapFilter();
		String isSuperAdminsLdapFilter = String.format("&(uid=%s)(%s)", uid, superAdminsLdapFilter);
		Iterable<UserLdap> isSuperAdmins = userLdapRepository.findAll(LdapQueryBuilder.query().filter(isSuperAdminsLdapFilter));
		return !IterableUtils.isEmpty(isSuperAdmins);
    }
    
	public Map<String,String> getMapUsersFromMapAttributes(String searchValue) throws InvalidNameException {
  	  	Map<String, String> usersMap = new HashMap<>();
  	  	List<UserLdap>  userLdaps =  this.getAllmembers(searchValue);
		for(UserLdap userLdap : userLdaps) {
  			usersMap.put(userLdap.getEppn(), userLdap.getPrenomNom());
  		  }
  	  	TreeMap<String, String> sortMap = new TreeMap<String, String>(usersMap);
  	  	return sortMap;
  }
	
	public List<UserLdap> getUserLdaps(String eppn, String uid) {
		List<UserLdap> userLdaps = new ArrayList<UserLdap>();
		String identifiant = (eppn != null)? eppn : uid;
		if(identifiant != null &&identifiant.startsWith(paramUtil.getGenericUser())) {
			String splitIdentifiant [] = identifiant.split("_");
			String prenom = StringUtils.capitalize(paramUtil.getGenericUser());
			String ctx = splitIdentifiant[1];
			UserLdap generic = new UserLdap();
			generic.setUid(identifiant);
			generic.setEppn(identifiant + "@" + nomDomaine);
			generic.setPrenomNom(StringUtils.capitalize(prenom) + " " + StringUtils.capitalize(ctx));
			userLdaps.add(generic);
		}else {
			if(uid !=null) {
				userLdaps = userLdapRepository.findByUid(uid);
			}else if(eppn != null) {
				userLdaps = userLdapRepository.findByEppnEquals(eppn);
			}
		}
		return userLdaps;
	}
          
}
