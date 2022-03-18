package org.esupportail.emargement.repositories;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionEpreuveRepository extends JpaRepository<SessionEpreuve, Long>{

	Long countByNomSessionEpreuve(String nomSessionEpreuve);
	
	Page<SessionEpreuve> findByNomSessionEpreuve(String nomSessionEpreuve, Pageable pageable);
	
	List<SessionEpreuve> findSessionEpreuveByIsSessionEpreuveClosedFalseOrderByDateExamen();
	
	List<SessionEpreuve>  findSessionEpreuveByDateExamenAndCampusEqualsAndIdNot(Date date, Campus campus, Long id);
	
	List<SessionEpreuve>  findSessionEpreuveByContext(Context context);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThanEqualAndDateExamenLessThanEqual(Date startDate, Date endDate);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThanEqualAndDateExamenLessThanEqualOrDateFinGreaterThanEqualAndDateFinLessThanEqual(Date startDate, Date endDate, Date startDateFin, Date endDateFin);
	
	List<SessionEpreuve> findAllByDateExamen(Date date);
	
	Long countByDateExamenGreaterThanEqual(Date date);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThan(Date date);
	
	List<SessionEpreuve> findAllByDateExamenOrDateFinNotNullAndDateFinLessThanEqualAndDateFinGreaterThanEqual(Date date, Date dateFin, Date dateFin2);
	
	List<SessionEpreuve> findAllByDateExamenLessThan(Date date);
	
	List<SessionEpreuve> findAllByDateArchivageIsNullOrderByNomSessionEpreuve();
	
	List<SessionEpreuve> findAByAnneeUnivAndDateArchivageIsNotNull(String anneeUniv);
	
	Page<SessionEpreuve> findAllByAnneeUniv(String anneeUniv, Pageable pageable);
	
	Long countByAnneeUniv(String anneeUniv);
	
	List<SessionEpreuve>  findByBlackListGroupe(Groupe groupe);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and person.eppn= :eppn and date_examen= :date", nativeQuery = true)
	Long countSessionEpreuveIdExpected(String eppn, Date date);
	
	@Query(value = "select * from session_epreuve "
			+ "where (date_examen >= :startDate and date_examen <= :endDate) or "
			+ " (date_fin >= :startDate and date_fin <= :endDate)", nativeQuery = true)
	List<SessionEpreuve> getAllSessionEpreuveForCalendar(Date startDate, Date endDate);
	
	@Query(value = "select session_epreuve.id from tag_check, person, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and person.eppn= :eppn and date_examen= :date and  heure_epreuve <= :now and fin_epreuve >= :now", nativeQuery = true)
	Long getSessionEpreuveIdExpected(String eppn, Date date, LocalTime now);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and person.eppn= :eppn and date_examen= :date and  heure_epreuve >= :now and "
			+ "heure_convocation <= :now and session_epreuve.id = :id", nativeQuery = true)
	Long checkIsBeforeSession(String eppn, Date date, LocalTime now, Long id);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and person.eppn= :eppn and date_examen <= :date and date_fin >= :dateFin "
			+ "and heure_epreuve >= :now and heure_convocation <= :now and session_epreuve.id = :id", nativeQuery = true)
	Long checkIsBeforeSessionWithDateFin(String eppn, Date date, Date dateFin, LocalTime now, Long id);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
	+ "where tag_check.person_id = person.id "
	+ "and session_epreuve.id = tag_check.session_epreuve_id "
	+ "and person.eppn= :eppn and date_examen= :date and  heure_convocation > :now "
	+ "and session_epreuve.id = :id", nativeQuery = true)
	Long checkIsBeforeConvocation(String eppn, Date date, LocalTime now, Long id);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
	+ "where tag_check.person_id = person.id "
	+ "and session_epreuve.id = tag_check.session_epreuve_id "
	+ "and person.eppn= :eppn and date_examen <= :date and date_fin >= :dateFin "
	+ "and heure_convocation > :now and session_epreuve.id = :id", nativeQuery = true)
	Long checkIsBeforeConvocationWithDateFin(String eppn, Date date, Date dateFin, LocalTime now, Long id);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
	+ "where tag_check.person_id = person.id "
	+ "and session_epreuve.id = tag_check.session_epreuve_id "
	+ "and person.eppn= :eppn and date_examen= :date and :now <= fin_epreuve "
	+ "and session_epreuve.id = :id", nativeQuery = true)
	Long checkIsBeforeFin(String eppn, Date date, LocalTime now, Long id);
	
	@Query(value = "select count(*) from tag_check, person, session_epreuve "
	+ "where tag_check.person_id = person.id "
	+ "and session_epreuve.id = tag_check.session_epreuve_id "
	+ "and person.eppn= :eppn and date_examen<= :date and date_fin >= :dateFin "
	+ "and :now <= fin_epreuve and session_epreuve.id = :id", nativeQuery = true)
	Long checkIsBeforeFinWithDateFin(String eppn, Date date, Date dateFin, LocalTime now, Long id);
	
	@Query(value = "select date_examen from session_epreuve where annee_univ = :anneeUniv and context_id = :ctxId order by date_examen limit 1;", nativeQuery = true)
	Date findFirstDateExamen(String anneeUniv, Long ctxId);
	
	@Query(value = "select date_examen from session_epreuve where annee_univ = :anneeUniv and context_id = :ctxId order by date_examen DESC limit 1;", nativeQuery = true)
	Date findLastDateExamen(String anneeUniv, Long ctxId);
	
	@Query(value = "select distinct annee_univ from session_epreuve where context_id = :ctxId order by annee_univ ", nativeQuery = true)
	List<String> findDistinctAnneeUniv(Long ctxId);
	
	@Query(value = "select distinct annee_univ from session_epreuve order by annee_univ", nativeQuery = true)
	List<String> findDistinctAnneeUnivAll();
	
	@Query(value = "select count(*) from session_epreuve where nom_session_epreuve=:nom", nativeQuery = true)
	Long countExistingNomSessionEpreuve(String nom);
	
	//STATS
	@Query(value = "select site, count(*) as count from session_epreuve, campus where "
			+ "session_epreuve.campus_id=campus.id and session_epreuve.context_id=:context "
			+ "and is_session_epreuve_closed = 't' and annee_univ like :anneeUniv group by site order by count desc;", nativeQuery = true)
	List<Object[]> countSessionEpreuveByCampus(Long context, String anneeUniv);
	
	@Query(value = "SELECT CAST(DATE_PART('month', date_examen) AS INTEGER) AS month, count(*) "
			+ "AS count FROM session_epreuve  WHERE context_id=:context AND is_session_epreuve_closed = 't' and annee_univ like :anneeUniv GROUP BY month", nativeQuery = true)
	List<Object[]> countSessionEpreuveByYearMonth(Long context, String anneeUniv);
	
	@Query(value = "select key, CASE WHEN is_session_epreuve_closed='t' THEN 'Fermée' ELSE 'Ouverte' END AS statut, count(*) as count "
			+ "from session_epreuve, context where session_epreuve.context_id=context.id and annee_univ like :anneeUniv group by key, statut order by key, statut, count", nativeQuery = true)
	List<Object[]> countAllSessionEpreuvesByContext(String anneeUniv);
	
	@Query(value = " select key, count(*) from session_epreuve, type_session where session_epreuve.type_session_id = type_session.id and session_epreuve.context_id=:context and is_session_epreuve_closed = 't' "
			+ "and annee_univ like :anneeUniv group by key", nativeQuery = true)
	List<Object[]> countSessionEpreuveByType(Long context, String anneeUniv);
	
	@Query(value = " select context.key as ctx, type_session.key as type, count(*) from session_epreuve, context, type_session where session_epreuve.type_session_id = type_session.id and "
			+ "	session_epreuve.context_id=context.id and is_session_epreuve_closed = 't' and annee_univ like :anneeUniv group by context.key, type_session.key order by ctx, type, count;", nativeQuery = true)
	List<Object[]> countSessionEpreuveByTypeByContext(String anneeUniv);

}
