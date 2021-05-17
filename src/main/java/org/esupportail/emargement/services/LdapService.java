package org.esupportail.emargement.services;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
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
	
	@Value("${ldap.people}")
	private String ldapPeople;
	
	@Value("${emargement.ruleSuperAdmin.memberOf}")
	private String ruleSuperAdmin;
	
	@Value("${emargement.ruleSuperAdmin.uid}")
	private String ruleSuperAdminUid;
	
	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	@Resource
	UserAppService userAppService;
	
	public void setLdapTemplate(LdapTemplate ldapTemplate) {
	      this.ldapTemplate = ldapTemplate;
	   }

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
		}else if(uid.startsWith(userAppService.getGenericUser())) {
			eppn= uid + "@" + nomDomaine;
		}
		return eppn;
	}
    
    public List<String> getAllGroupNames(String searchValue) throws InvalidNameException {
		LdapName toto = new LdapName(ldapGroups);
		String searchfilter = (searchValue.isEmpty())? "*": "*".concat(searchValue).concat("*");
		return ldapTemplate.search(
				query().base(toto).searchScope(SearchScope.SUBTREE).timeLimit(THREE_SECONDS).where("cn").like(searchfilter),
				new AttributesMapper<String>() {
					public String mapFromAttributes(Attributes attrs) throws NamingException {
						return attrs.get("cn").get().toString();
					}
				});
    }
    
    public List<Map<String, List<String>>>  getAllmembers(String searchValue, boolean unique) throws InvalidNameException {
    	String searchUsers = "memberOf=cn=" + searchValue + "," + ldapGroups;
    	if(unique) {
    		searchUsers = "memberOf=" + ruleSuperAdmin;
    	}
    	return  ldapTemplate.search(ldapPeople,searchUsers,
          new AttributesMapper<Map<String, List<String>>>() {
              @Override
              public Map<String, List<String>> mapFromAttributes(Attributes attributes) throws NamingException {
                  Map<String, List<String>> attrsMap = new HashMap<>();
                  NamingEnumeration<String> attrIdEnum = attributes.getIDs();
                  while (attrIdEnum.hasMoreElements()) {
                      // Get attribute id:
                      String attrId = attrIdEnum.next();
                      // Get all attribute values:
                      Attribute attr = attributes.get(attrId);
                      NamingEnumeration<?> attrValuesEnum = attr.getAll();
                      while (attrValuesEnum.hasMore()) {
                          if (!attrsMap.containsKey(attrId))
                              attrsMap.put(attrId, new ArrayList<String>()); 
                          attrsMap.get(attrId).add(attrValuesEnum.next().toString());
                      }
                  }
                  return attrsMap;
              }
          });
    }
    
    public Boolean checkIsUserInGroupSuperAdminLdap(String searchValue) throws InvalidNameException {
    	
    	boolean isSuperAdmin = false;
    	
    	if(!ruleSuperAdminUid.isEmpty()) {
			String [] splitValues = ruleSuperAdminUid.split(",");
			List<String> list = Arrays.asList(splitValues);
			list.replaceAll(String::trim);
			if(list.contains(searchValue)){
	        	isSuperAdmin = true;
			}
    	}else {
	    	List<Map<String, List<String>>>  list =  this.getAllmembers(searchValue, true);
	    	
	    	if(list.size()>0) {//TO DO optimiser ....
	    		for(Map<String, List<String>> map : list ) {
	    			for (Map.Entry<String, List<String>> map2 : map.entrySet()) {
	    		        if("uid".equals(map2.getKey()) && searchValue.equals(map2.getValue().get(0))) {
	    		        	isSuperAdmin = true;
	    		        	break;
	    		        }
	    		       /* if("eduPersonPrincipalName".equals(map2.getKey()) && searchValue.equals(map2.getValue())) {
	    		        	isSuperAdmin = true;
	    		        	break;
	    		        }*/
	    		    }
	    		}
	    	}
    	}
    	return isSuperAdmin;
    }
    
	public Map<String,String> getMapUsersFromMapAttributes(String searchValue) throws InvalidNameException {
  	  Map<String, String> usersMap = new HashMap<>();
  	  
  	  List<Map<String, List<String>>>  list =  this.getAllmembers(searchValue, false);
  	  if(!list.isEmpty()){
  		  for(Map<String, List<String>> map : list) {
  			usersMap.put(map.get("eduPersonPrincipalName").get(0), map.get("displayName").get(0));
  		  }
  	  }
  	  TreeMap<String, String> sortMap = new TreeMap<String, String>(usersMap);
  	  return sortMap;
  }
	
	public List<UserLdap> getUserLdaps(String eppn, String uid) {
		
		List<UserLdap> userLdaps = new ArrayList<UserLdap>();
		String identifiant = (eppn != null)? eppn : uid;
		
		if(identifiant.startsWith(userAppService.getGenericUser())) {
			String splitIdentifiant [] = identifiant.split("_");
			String prenom = StringUtils.capitalize(userAppService.getGenericUser());
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
