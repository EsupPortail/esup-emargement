package org.esupportail.emargement.config;

import javax.servlet.http.HttpSessionEvent;

import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.security.ContextCasAuthenticationProvider;
import org.esupportail.emargement.security.ContextUserDetailsService;
import org.esupportail.emargement.services.LdapService;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener;
import org.jasig.cas.client.validation.Cas30ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="cas")
@PropertySource(value = "classpath:/emargement.properties", encoding = "UTF-8")
public class CasConfig {

	String url;

	String service;

	String key;

	public String getUrl() {
		return url;
	}

	public String getService() {
		return service;
	}

	public String getKey() {
		return key;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Bean
	public ServiceProperties serviceProperties() {
		ServiceProperties serviceProperties = new ServiceProperties();
		serviceProperties.setService(service + "/login/cas");
		serviceProperties.setSendRenew(false);
		return serviceProperties;
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint(ServiceProperties sP) {
		CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
		entryPoint.setLoginUrl(url + "/login");
		entryPoint.setServiceProperties(sP);
		return entryPoint;
	}

	@Bean
	public TicketValidator ticketValidator() {
		return new Cas30ServiceTicketValidator(url);
	}

	@Bean
	public ContextCasAuthenticationProvider casAuthenticationProvider(EmargementConfig config, UserAppRepository userAppRepository, ContextRepository contextRepository,
																	  LdapService ldapService, PersonRepository personRepository,
																	  ServiceProperties serviceProperties, TicketValidator ticketValidator) {
		ContextCasAuthenticationProvider provider = new ContextCasAuthenticationProvider();
		provider.setServiceProperties(serviceProperties);
		provider.setTicketValidator(ticketValidator);
		provider.setAuthenticationUserDetailsService(new ContextUserDetailsService(config, userAppRepository, contextRepository, ldapService, personRepository));
		provider.setKey(key);
		return provider;
	}


	@Bean
	public SecurityContextLogoutHandler securityContextLogoutHandler() {
		return new SecurityContextLogoutHandler();
	}

	@Bean
	public LogoutFilter logoutFilter() {
		LogoutFilter logoutFilter = new LogoutFilter(
				url + "/logout?service=" + service, securityContextLogoutHandler());
		logoutFilter.setFilterProcessesUrl("/logout");
		return logoutFilter;
	}

	@Bean
	public SingleSignOutFilter singleSignOutFilter() {
		SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
		singleSignOutFilter.setIgnoreInitConfiguration(true);
		return singleSignOutFilter;
	}

	@EventListener
	public SingleSignOutHttpSessionListener singleSignOutHttpSessionListener(HttpSessionEvent event) {
		return new SingleSignOutHttpSessionListener();
	}
}
