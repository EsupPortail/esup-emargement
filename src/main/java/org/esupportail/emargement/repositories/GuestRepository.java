package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
	
	List<Guest> findByEmail(String email);
    
}
