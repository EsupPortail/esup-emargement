package org.esupportail.emargement.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.authentication.NullStatelessTicketCache;
import org.springframework.security.cas.authentication.StatelessTicketCache;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;



/**
 * @See CasAuthenticationProvider 
 * 
 * ContextCasAuthenticationProvider == CasAuthenticationProvider
 * 
 * Goal is just to use ContextCasAuthenticationToken instead of CasAuthenticationToken
 *  
 * BUT because of usage of *private* on methods and attributes, we re-copy all CasAuthenticationProvider here !
 *
 */
public class ContextCasAuthenticationProvider implements AuthenticationProvider,
InitializingBean, MessageSourceAware {
	// ~ Static fields/initializers
	// =====================================================================================

	private static final Log logger = LogFactory.getLog(CasAuthenticationProvider.class);

	// ~ Instance fields
	// ================================================================================================

	private AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;

	private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
	private StatelessTicketCache statelessTicketCache = new NullStatelessTicketCache();
	private String key;
	private TicketValidator ticketValidator;
	private ServiceProperties serviceProperties;
	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

	// ~ Methods
	// ========================================================================================================

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.authenticationUserDetailsService,
				"An authenticationUserDetailsService must be set");
		Assert.notNull(this.ticketValidator, "A ticketValidator must be set");
		Assert.notNull(this.statelessTicketCache, "A statelessTicketCache must be set");
		Assert.hasText(
				this.key,
				"A Key is required so CasAuthenticationProvider can identify tokens it previously authenticated");
		Assert.notNull(this.messages, "A message source must be set");
	}

	@Transactional
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		if (!supports(authentication.getClass())) {
			return null;
		}

		if (authentication instanceof UsernamePasswordAuthenticationToken
				&& (!CasAuthenticationFilter.CAS_STATEFUL_IDENTIFIER
						.equals(authentication.getPrincipal().toString()) && !CasAuthenticationFilter.CAS_STATELESS_IDENTIFIER
						.equals(authentication.getPrincipal().toString()))) {
			// UsernamePasswordAuthenticationToken not CAS related
			return null;
		}

		// If an existing ContextCasAuthenticationToken, just check we created it
		if (authentication instanceof ContextCasAuthenticationToken) {
			if (this.key.hashCode() == ((ContextCasAuthenticationToken) authentication)
					.getKeyHash()) {
				return authentication;
			}
			else {
				throw new BadCredentialsException(
						messages.getMessage("CasAuthenticationProvider.incorrectKey",
								"The presented ContextCasAuthenticationToken does not contain the expected key"));
			}
		}

		// Ensure credentials are presented
		if ((authentication.getCredentials() == null)
				|| "".equals(authentication.getCredentials())) {
			throw new BadCredentialsException(messages.getMessage(
					"CasAuthenticationProvider.noServiceTicket",
					"Failed to provide a CAS service ticket to validate"));
		}

		boolean stateless = false;

		if (authentication instanceof UsernamePasswordAuthenticationToken
				&& CasAuthenticationFilter.CAS_STATELESS_IDENTIFIER.equals(authentication
						.getPrincipal())) {
			stateless = true;
		}

		ContextCasAuthenticationToken result = null;

		if (stateless) {
			// Try to obtain from cache
			result = (ContextCasAuthenticationToken)statelessTicketCache.getByTicketId(authentication.getCredentials()
					.toString());
		}

		if (result == null) {
			result = this.authenticateNow(authentication);
			result.setDetails(authentication.getDetails());
		}

		if (stateless) {
			// Add to cache
			statelessTicketCache.putTicketInCache(result);
		}

		return result;
	}

	private ContextCasAuthenticationToken authenticateNow(final Authentication authentication)
			throws AuthenticationException {
		try {
			final Assertion assertion = this.ticketValidator.validate(authentication
					.getCredentials().toString(), getServiceUrl(authentication));
			final UserDetails userDetails = loadUserByAssertion(assertion);
			userDetailsChecker.check(userDetails);
			return new ContextCasAuthenticationToken(this.key, userDetails,
					authentication.getCredentials(),
					authoritiesMapper.mapAuthorities(userDetails.getAuthorities()),
					userDetails, assertion);
		}
		catch (final TicketValidationException e) {
			throw new BadCredentialsException(e.getMessage(), e);
		}
	}

	/**
	 * Gets the serviceUrl. If the {@link Authentication#getDetails()} is an instance of
	 * {@link ServiceAuthenticationDetails}, then
	 * {@link ServiceAuthenticationDetails#getServiceUrl()} is used. Otherwise, the
	 * {@link ServiceProperties#getService()} is used.
	 *
	 * @param authentication
	 * @return
	 */
	private String getServiceUrl(Authentication authentication) {
		String serviceUrl;
		if (authentication.getDetails() instanceof ServiceAuthenticationDetails) {
			serviceUrl = ((ServiceAuthenticationDetails) authentication.getDetails())
					.getServiceUrl();
		}
		else if (serviceProperties == null) {
			throw new IllegalStateException(
					"serviceProperties cannot be null unless Authentication.getDetails() implements ServiceAuthenticationDetails.");
		}
		else if (serviceProperties.getService() == null) {
			throw new IllegalStateException(
					"serviceProperties.getService() cannot be null unless Authentication.getDetails() implements ServiceAuthenticationDetails.");
		}
		else {
			serviceUrl = serviceProperties.getService();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("serviceUrl = " + serviceUrl);
		}
		return serviceUrl;
	}

	/**
	 * Template method for retrieving the UserDetails based on the assertion. Default is
	 * to call configured userDetailsService and pass the username. Deployers can override
	 * this method and retrieve the user based on any criteria they desire.
	 *
	 * @param assertion The CAS Assertion.
	 * @return the UserDetails.
	 */
	protected UserDetails loadUserByAssertion(final Assertion assertion) {
		final CasAssertionAuthenticationToken token = new CasAssertionAuthenticationToken(
				assertion, "");
		return this.authenticationUserDetailsService.loadUserDetails(token);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Sets the UserDetailsService to use. This is a convenience method to invoke
	 */
	public void setUserDetailsService(final UserDetailsService userDetailsService) {
		this.authenticationUserDetailsService = new UserDetailsByNameServiceWrapper(
				userDetailsService);
	}

	public void setAuthenticationUserDetailsService(
			final AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService) {
		this.authenticationUserDetailsService = authenticationUserDetailsService;
	}

	public void setServiceProperties(final ServiceProperties serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	protected String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public StatelessTicketCache getStatelessTicketCache() {
		return statelessTicketCache;
	}

	protected TicketValidator getTicketValidator() {
		return ticketValidator;
	}

	public void setMessageSource(final MessageSource messageSource) {
		this.messages = new MessageSourceAccessor(messageSource);
	}

	public void setStatelessTicketCache(final StatelessTicketCache statelessTicketCache) {
		this.statelessTicketCache = statelessTicketCache;
	}

	public void setTicketValidator(final TicketValidator ticketValidator) {
		this.ticketValidator = ticketValidator;
	}

	public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		this.authoritiesMapper = authoritiesMapper;
	}

	public boolean supports(final Class<?> authentication) {
		return (UsernamePasswordAuthenticationToken.class
				.isAssignableFrom(authentication))
				|| (ContextCasAuthenticationToken.class.isAssignableFrom(authentication))
				|| (CasAssertionAuthenticationToken.class
						.isAssignableFrom(authentication));
	}
}
