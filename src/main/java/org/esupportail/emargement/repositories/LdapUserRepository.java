package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.LdapUser;
import org.springframework.data.ldap.repository.LdapRepository;

public interface LdapUserRepository extends LdapRepository<LdapUser> {

	List<LdapUser> findByNameLikeIgnoreCase(String username);
	
	List<LdapUser> findByEppnEquals(String eppn);
	
	List<LdapUser> findByNumEtudiantEquals(String numEtudiant);
	
	List<LdapUser> findByNameContainingIgnoreCase(String username);
	
	List<LdapUser> findByNomPrenomContainingIgnoreCase(String cn);
	
}