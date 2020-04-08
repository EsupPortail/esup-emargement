package org.esupportail.emargement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="ldap")
public class LdapConfig {
	
	private String url;
	
	private String username;
	
	private String password;
	
	private String userSearchFilter;
	
	private String people;

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String user) {
		this.username = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUserSearchFilter(String userSearchFilter) {
		this.userSearchFilter = userSearchFilter;
	}

	public void setPeople(String ouPeople) {
		this.people = ouPeople;
	}

	@Bean
	public DefaultSpringSecurityContextSource ldapContextSource() throws Exception {
		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
		contextSource.setUserDn(username);
		contextSource.setPassword(password);
		return contextSource;
	}

	@Bean
	public LdapUserSearch ldapUserSearch() throws Exception {
		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(people, userSearchFilter,
				ldapContextSource());
		return ldapUserSearch;
	}
	
}

