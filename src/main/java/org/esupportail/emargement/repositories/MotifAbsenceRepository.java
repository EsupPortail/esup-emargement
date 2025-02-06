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
	
	List<MotifAbsence> findByIsActifTrueOrderByLibelle();
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueOrderByLibelle();
	
	List<MotifAbsence> findByIsActifTrueAndStatutAbsenceAndTypeAbsenceOrderByLibelle(StatutAbsence statut, TypeAbsence type);
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueAndStatutAbsenceAndTypeAbsenceOrderByLibelle(StatutAbsence statut, TypeAbsence type);
	
	List<MotifAbsence> findByIsActifTrueAndStatutAbsenceOrderByLibelle(StatutAbsence statut);
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueAndStatutAbsenceOrderByLibelle(StatutAbsence statut);
	
	List<MotifAbsence> findByIsActifTrueAndTypeAbsenceOrderByLibelle(TypeAbsence typeAbsence);
	
	List<MotifAbsence> findByIsActifTrueAndIsTagCheckerVisibleTrueAndTypeAbsenceOrderByLibelle(TypeAbsence typeAbsence);
}