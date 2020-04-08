package org.esupportail.emargement.domain;

import javax.naming.directory.Attributes;

import org.springframework.ldap.core.AttributesMapper;

public class PersonAttributMapper implements AttributesMapper {
	
	public UserLdap mapFromAttributes(Attributes attrs)
			throws javax.naming.NamingException {
		UserLdap p = new UserLdap();
		if (null!=attrs.get("uid")) {
			p.setUid(attrs.get("uid").get().toString());
		}
		if (null!=attrs.get("mail")) {
			p.setEmail(attrs.get("mail").get().toString());
		}
		if (null!=attrs.get("eduPersonPrincipalName")) {
			p.setEppn(attrs.get("eduPersonPrincipalName").get().toString());
		}
		if (null!=attrs.get("eduPersonPrimaryAffiliation")) {
		}
		return p;
	}
}

