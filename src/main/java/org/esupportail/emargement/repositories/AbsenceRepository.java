package org.esupportail.emargement.repositories;

import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long>{
	List<Absence> findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(Date date, Date date2);
	 
	@Query("SELECT a FROM Absence a WHERE a.dateDebut <= :endDate AND a.dateFin >= :startDate")
	List<Absence> findAbsencesWithinDateRange(Date startDate, Date endDate);
}
