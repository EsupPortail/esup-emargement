package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.LdapUser;
import org.springframework.data.ldap.repository.LdapRepository;

public interface LdapUserRepository extends LdapRepository<LdapUser> {

	List<LdapUser> findByUsernameLikeIgnoreCase(String username);
	
	List<LdapUser> findByEppnEquals(String eppn);
	
	List<LdapUser> findByNumEtudiantEquals(String numEtudiant);
	
	List<LdapUser> findByUsernameContainingIgnoreCase(String username);
	
	List<LdapUser> findByNomPrenomContainingIgnoreCase(String cn);
	
}