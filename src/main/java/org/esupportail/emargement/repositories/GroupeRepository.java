package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
	
	List<Groupe> findByNomLikeIgnoreCase(String nom);
	
	List<Groupe> findByNomLikeIgnoreCaseAndContext(String nom, Context ctx);
	
	List<Groupe> findByAnneeUnivOrderByNom(String annee);
	
	List<Groupe> findAllByOrderByNom();
	
	List<Groupe> findByContext(Context context);
	
	Long countByNom(String nom);

	@Query("select count(p) > 0	from Groupe g " +
			"join g.persons p " +
			"where g.id = :groupeId " +
			"and p.eppn = :eppn")
			boolean isPersonInGroup(
					 @Param("groupeId") Long groupeId,
			        @Param("eppn") String eppn);
}
