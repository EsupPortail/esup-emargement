package org.esupportail.emargement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.stereotype.Component;

@Component
@EnableLdapRepositories
public class LdapConfig {

	private String userSearchFilter;

	@Value("${ldap.userSearchFilter}")
	public void setUserSearchFilter(String userSearchFilter) {
		this.userSearchFilter = userSearchFilter;
	}

	public String getUserSearchFilter() {
		return userSearchFilter;
	}
}

