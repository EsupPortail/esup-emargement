package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.MotifAbsence;
import org.esupportail.emargement.domain.MotifAbsence.StatutAbsence;
import org.esupportail.emargement.domain.MotifAbsence.TypeAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotifAbsenceRepository extends JpaRepository<MotifAbsence, Long>{

	List<MotifAbsence> findByContextKey(String key);
	
	List<MotifAbsence> findByIsActifTrue();
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrue();
	
	List<MotifAbsence> findByIsActifTrueAndStatutAbsenceAndTypeAbsence(StatutAbsence statut, TypeAbsence type);
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueAndStatutAbsenceAndTypeAbsence(StatutAbsence statut, TypeAbsence type);
	
	List<MotifAbsence> findByIsActifTrueAndStatutAbsence(StatutAbsence statut);
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueAndStatutAbsence(StatutAbsence statut);
	
	List<MotifAbsence> findByIsActifTrueAndTypeAbsence(TypeAbsence typeAbsence);
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueAndTypeAbsence(TypeAbsence typeAbsence);
}