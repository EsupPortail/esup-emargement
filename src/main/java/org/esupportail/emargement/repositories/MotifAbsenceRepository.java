package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.MotifAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotifAbsenceRepository extends JpaRepository<MotifAbsence, Long>{

	List<MotifAbsence> findByContextKey(String key);
	
	List<MotifAbsence> findByIsActifTrue();
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrue();
}
