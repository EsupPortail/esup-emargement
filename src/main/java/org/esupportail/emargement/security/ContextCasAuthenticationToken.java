package org.esupportail.emargement.security;

import java.util.Collection;

import org.jasig.cas.client.validation.Assertion;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("serial")
public class ContextCasAuthenticationToken extends CasAuthenticationToken {

	public ContextCasAuthenticationToken(String key, Object principal, Object credentials,
			Collection<? extends GrantedAuthority> authorities, UserDetails userDetails, Assertion assertion) {
		super(key, principal, credentials, authorities, userDetails, assertion);
	}
	

	@SuppressWarnings("unchecked")
	public Collection<GrantedAuthority> getAuthorities() {
		return (Collection<GrantedAuthority>)this.getUserDetails().getAuthorities();
	}

}
