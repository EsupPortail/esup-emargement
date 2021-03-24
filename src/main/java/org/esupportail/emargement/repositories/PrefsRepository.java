package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Prefs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrefsRepository extends JpaRepository<Prefs, Long> {
	
	List<Prefs> findByUserAppEppnAndNom(String eppn, String nom);
	
	List<Prefs> findByContext(Context context);

}
