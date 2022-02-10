package org.esupportail.emargement.services;

import java.util.List;
import java.util.Random;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
public class UserService {
	
	@Autowired	
	TagCheckRepository tagCheckRepository;
	
	@Resource
	LdapService ldapService;

	public Page<TagCheck> getTagChecks( Pageable pageable) {
		Page<TagCheck> tcs = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		if(!eppn.isEmpty()) {
			tcs = tagCheckRepository.findTagCheckByPersonEppn(eppn, pageable);
		}
		return tcs;
	}
	
	public String generateSessionToken(String idString) {  
		Random random = new Random(System.currentTimeMillis());  
		String sessionToken = idString + "token" + Math.abs(random.nextInt());
	
		while((tagCheckRepository.countTagCheckBysessionTokenEquals(sessionToken) > 0)) {
			sessionToken = idString +  "token" + Math.abs(random.nextInt());
		}
		return sessionToken;
	}
}
