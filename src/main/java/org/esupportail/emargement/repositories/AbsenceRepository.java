package org.esupportail.emargement.repositories;

import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long>{
	
	List<Absence> findByContext(Context context);
	
	List<Absence> findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(Date date, Date date2);
	 
	@Query("SELECT a FROM Absence a WHERE a.dateDebut <= :endDate AND a.dateFin >= :startDate")
	List<Absence> findAbsencesWithinDateRange(Date startDate, Date endDate);
	
	@Query("SELECT a FROM Absence a WHERE a.person = :person AND (a.dateFin >= :startDate AND a.dateDebut <= :endDate "
			+ " AND a.heureFin > :heureDebut " 
			+ " AND a.heureDebut < :heureFin AND a.context = :context)")
	List<Absence> findOverlappingAbsences(Person person,
	                                      Date startDate,
	                                      Date endDate,
	                                      Date heureDebut,
	                                      Date heureFin,
	                                      Context context);

}
