package org.esupportail.emargement.services;

import java.util.List;

import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	
	@Autowired
	private UserLdapRepository userLdapRepository;
	
	@Autowired	
	TagCheckRepository tagCheckRepository;

	public Page<TagCheck> getTagChecks( Pageable pageable) {
		Page<TagCheck> tcs = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<UserLdap> userLdap = (auth!=null)?  userLdapRepository.findByUid(auth.getName()) : null;
		String eppn = (userLdap != null)?  userLdap.get(0).getEppn()  : "";
		if(!eppn.isEmpty()) {
			tcs = tagCheckRepository.findTagCheckByPersonEppn(eppn, pageable);
		}
		return tcs;
	}
}
