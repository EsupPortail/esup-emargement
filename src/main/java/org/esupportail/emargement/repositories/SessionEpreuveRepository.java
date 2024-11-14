package org.esupportail.emargement.repositories;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionEpreuveRepository extends JpaRepository<SessionEpreuve, Long>,JpaSpecificationExecutor<SessionEpreuve>{

	Long countByNomSessionEpreuve(String nomSessionEpreuve);
	
	Long countByAdeEventId(Long id);
	
	Long countByNomSessionEpreuveAndDateExamenAndHeureEpreuveAndFinEpreuve(String nomSessionEpreuve, Date dateExamen, Date heureDebut, Date heureFin);
	
	List<SessionEpreuve> findByAdeEventId(Long id);
	
	List<SessionEpreuve> findByIdIn(List<Long> id);
	
	List<SessionEpreuve> findSessionEpreuveByStatutNotInOrderByDateExamen(List<Statut> statuts);
	
	List<SessionEpreuve>  findSessionEpreuveByContext(Context context);
	
	List<SessionEpreuve>  findByContextAndDateExamenGreaterThanEqual(Context context, Date today);
	
	List<SessionEpreuve>  findByContextAndDateExamenGreaterThanEqualAndDateExamenLessThanEqual(Context context, Date today, Date endDate);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThanEqualAndDateExamenLessThanEqual(Date startDate, Date endDate);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThanEqualAndDateExamenLessThanEqualOrDateFinGreaterThanEqualAndDateFinLessThanEqual(Date startDate, Date endDate, Date startDateFin, Date endDateFin);
	
	List<SessionEpreuve> findAllByDateExamen(Date date);

	List<SessionEpreuve> findAllByDateExamenLessThanEqualAndDateFinGreaterThanEqualOrDateExamenGreaterThanEqualAndDateExamenLessThanEqualOrDateFinGreaterThanEqualAndDateFinLessThanEqual(Date startDate1, Date endDate1, Date startDate, Date endDate, Date startDateFin, Date endDateFin);
	
	Long countByDateExamenGreaterThanEqual(Date date);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThan(Date date);
	
	List<SessionEpreuve> findByDateExamenLessThanAndDateFinIsNullAndStatutNotOrDateFinLessThanAndStatutNot(Date date, Statut statut, Date date2, Statut statut2);
	
	List<SessionEpreuve> findAllByDateExamenOrDateFinNotNullAndDateFinLessThanEqualAndDateFinGreaterThanEqual(Date date, Date dateFin, Date dateFin2);
	
	List<SessionEpreuve> findAllByDateExamenLessThan(Date date);
	
	List<SessionEpreuve> findAllByDateArchivageIsNullOrderByNomSessionEpreuve();
	
	List<SessionEpreuve> findAByAnneeUnivAndDateArchivageIsNotNull(String anneeUniv);
	
	Long countByAnneeUniv(String anneeUniv);
	
	List<SessionEpreuve>  findByBlackListGroupe(Groupe groupe);
	
	List<SessionEpreuve> findDistinctByNomSessionEpreuveLikeIgnoreCase(String nom);
	
	List<SessionEpreuve> findByNomSessionEpreuveLikeIgnoreCase(String nom);
	
	List<SessionEpreuve> findByNomSessionEpreuve(String nom);
	
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
	        + "and person.eppn = :eppn "
	        + "and ((date_fin is null and date_examen = :date) or (date_examen <= :date and  date_fin >= :date)) "
	        + "and heure_epreuve <= :now "
	        + "and (fin_epreuve >= :now or fin_epreuve is null)", nativeQuery = true)
	List<Long> getSessionEpreuveIdExpected(String eppn, Date date, LocalTime now);
	
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
	
	@Query(value = "select distinct type_session.id,libelle from session_epreuve, type_session where  session_epreuve.type_session_id = type_session.id and session_epreuve.context_id = :ctxId", nativeQuery = true)
	List<Object[]> findDistinctTypeSession(Long ctxId);
	
	@Query(value = "select distinct annee_univ from session_epreuve order by annee_univ", nativeQuery = true)
	List<String> findDistinctAnneeUnivAll();
		
	//STATS
	@Query(value = "select site, count(*) as count from session_epreuve, campus where "
			+ "session_epreuve.campus_id=campus.id and session_epreuve.context_id=:context "
			+ "and statut = 'CLOSED' and annee_univ like :anneeUniv group by site order by count desc;", nativeQuery = true)
	List<Object[]> countSessionEpreuveByCampus(Long context, String anneeUniv);
	
	@Query(value = "SELECT CAST(DATE_PART('month', date_examen) AS INTEGER) AS month, count(*) "
			+ "AS count FROM session_epreuve  WHERE context_id=:context AND statut = 'CLOSED' and annee_univ like :anneeUniv GROUP BY month", nativeQuery = true)
	List<Object[]> countSessionEpreuveByYearMonth(Long context, String anneeUniv);
	
	@Query(value = "select key, CASE WHEN statut = 'CLOSED' THEN 'Fermée' ELSE 'Ouverte' END AS statut, count(*) as count "
			+ "from session_epreuve, context where session_epreuve.context_id=context.id and annee_univ like :anneeUniv group by key, statut order by key, statut, count", nativeQuery = true)
	List<Object[]> countAllSessionEpreuvesByContext(String anneeUniv);
	
	@Query(value = " select key, count(*) from session_epreuve, type_session where session_epreuve.type_session_id = type_session.id and session_epreuve.context_id=:context and statut = 'CLOSED' "
			+ "and annee_univ like :anneeUniv group by key", nativeQuery = true)
	List<Object[]> countSessionEpreuveByType(Long context, String anneeUniv);
	
	@Query(value = " select context.key as ctx, type_session.key as type, count(*) from session_epreuve, context, type_session where session_epreuve.type_session_id = type_session.id and "
			+ "	session_epreuve.context_id=context.id and statut = 'CLOSED' and annee_univ like :anneeUniv group by context.key, type_session.key order by ctx, type, count;", nativeQuery = true)
	List<Object[]> countSessionEpreuveByTypeByContext(String anneeUniv);
	
	List<SessionEpreuve> findByContextOrderByDateExamenDescHeureEpreuveAscFinEpreuveAsc(Context ctx);

	 @Query(value = "SELECT * FROM session_epreuve WHERE context_id = :contextId AND id NOT IN " +
             "(SELECT DISTINCT session_epreuve_id FROM tag_check WHERE context_id = :contextId) " +
             "AND ((DATE(date_examen) < DATE(:date) AND date_fin IS NULL) " +
             "OR (date_fin IS NOT NULL AND DATE(date_fin) < DATE(:date)))",
     nativeQuery = true)
	List<SessionEpreuve> findSessionEpreuveWithNoTagCheck(Date date, Long contextId);
	
	@Query(value = "select * from session_epreuve where context_id =  :contextId and id not in "
			+ "(select distinct session_epreuve_id from tag_checker, session_location "
			+ "where session_location.id = tag_checker.session_location_id and tag_checker.context_id =  :contextId  and session_location.context_id =  :contextId) "
			+ "and ((DATE(date_examen) < DATE(:date) AND date_fin IS NULL) OR (date_fin IS NOT NULL AND DATE(date_fin) < DATE(:date)))", nativeQuery = true)
	List<SessionEpreuve> findSessionEpreuveWithNoTagChecker(Date date, Long contextId);
	
	@Query(value = "select * from session_epreuve where context_id = :contextId and id not in "
			+ "(select distinct session_epreuve_id from session_location where context_id = :contextId) "
			+ "and ((DATE(date_examen) < DATE(:date) AND date_fin IS NULL) OR (date_fin IS NOT NULL AND DATE(date_fin) < DATE(:date)))", nativeQuery = true)
	List<SessionEpreuve> findSessionEpreuveWithNoSessionLocation(Date date, Long contextId);
	
	@Query(value = "select * from session_epreuve where context_id =  :contextId and statut NOT LIKE 'CANCELLED' and id in "
			+ "(SELECT session_epreuve_id FROM tag_check where context_id = :contextId GROUP BY session_epreuve_id "
			+ "HAVING COUNT(tag_date) = 0 OR COUNT(*) = SUM(CASE WHEN tag_date IS NULL THEN 1 ELSE 0 END)) "
			+ "and ((DATE(date_examen) < DATE(:date) AND date_fin IS NULL) OR (date_fin IS NOT NULL AND DATE(date_fin) < DATE(:date)))", nativeQuery = true)
	List<SessionEpreuve> findSessionEpreuveWithNoTagDate(Date date, Long contextId);
}
