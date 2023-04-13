package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
	
	List<Groupe> findByNom(String nom);
	
	List<Groupe> findByNomLike(String nom);
	
	List<Groupe> findAllByOrderByNom();
	
	List<Groupe> findByContext(Context context);
	
	Long countByNom(String nom);
	
	Long countByPersonsIn(List<Person> persons);
	
	Long countByGuestsIn(List<Guest> guests);
    
}
