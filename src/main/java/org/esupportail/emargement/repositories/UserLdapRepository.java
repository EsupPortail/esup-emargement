package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.UserLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLdapRepository extends LdapRepository<UserLdap> {

	List<UserLdap> findByUid(String uid);
	
	List<UserLdap> findByUsernameLikeIgnoreCase(String username);
	
	List<UserLdap> findByEppnEquals(String eppn);
	
	List<UserLdap> findByNumEtudiantEquals(String numEtudiant);
	
	List<UserLdap> findByUsernameContainingIgnoreCase(String username);
	
	List<UserLdap> findByNomPrenomContainingIgnoreCase(String cn);
	
}