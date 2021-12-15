package org.esupportail.emargement.utils;

import org.springframework.stereotype.Component;

@Component
public class ParamUtil {
	
	private final String GENERIC_USER = "emargement";
	
	public String getGenericUser() {
		return GENERIC_USER;
	}

}
