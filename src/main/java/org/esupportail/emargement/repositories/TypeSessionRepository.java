package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.TypeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeSessionRepository extends JpaRepository<TypeSession, Long> {
    
	Long countByKey(String key);
	
	List<TypeSession> findAllByOrderByLibelle();
}
