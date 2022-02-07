package org.esupportail.emargement.services;

import org.apache.commons.collections4.IterableUtils;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Service
public class LdapGroupService {

    private static final Integer THREE_SECONDS = 3000;

    @Value("${ldap.groups}")
    private String ldapGroups;

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private LdapUserRepository ldapUserRepository;

    @Autowired
    LdapContextSource contextSource;

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

    List<LdapUser> getLdapMembers(String searchValue) throws InvalidNameException {
        String searchUsers = "";
        if(!searchValue.trim().isEmpty()) {
            searchUsers = String.format("(memberOf=cn=%s,%s,%s)", searchValue, ldapGroups, contextSource.getBaseLdapName());
        }
        return IterableUtils.toList(ldapUserRepository.findAll(LdapQueryBuilder.query().filter(searchUsers)));
    }


    public Map<String,String> getLdapMembersAsMap(String searchValue) throws InvalidNameException {
        Map<String, String> usersMap = new HashMap<>();
        List<LdapUser> ldapUsers =  this.getLdapMembers(searchValue);
        for(LdapUser ldapUser : ldapUsers) {
            usersMap.put(ldapUser.getEppn(), ldapUser.getPrenomNom());
        }
        TreeMap<String, String> sortMap = new TreeMap<String, String>(usersMap);
        return sortMap;
    }

}
