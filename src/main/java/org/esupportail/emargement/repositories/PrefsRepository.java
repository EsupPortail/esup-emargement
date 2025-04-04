package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Prefs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PrefsRepository extends JpaRepository<Prefs, Long> {
	
	List<Prefs> findByUserAppEppnAndNom(String eppn, String nom);
	
	List<Prefs> findByNom(String nom);
	
	List<Prefs> findByNomAndContext(String nom, Context ctx);
	
	List<Prefs> findByContext(Context context);
	
	List<Prefs> findByUserAppEppnAndNomLike(String eppn, String nom);
	
	@Query(value = "SELECT * from Prefs WHERE nom = :nom", nativeQuery = true)
	List<Prefs> findByNomAllContexts(String nom);
}
