package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
	
	List<Groupe> findByNomLikeIgnoreCase(String nom);
	
	List<Groupe> findByAnneeUnivOrderByNom(String annee);
	
	List<Groupe> findAllByOrderByNom();
	
	List<Groupe> findByContext(Context context);
	
	Long countByNom(String nom);
}
