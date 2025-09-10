package org.esupportail.emargement.repositories;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionEpreuveRepository extends JpaRepository<SessionEpreuve, Long>,JpaSpecificationExecutor<SessionEpreuve>{

	Long countByNomSessionEpreuve(String nomSessionEpreuve);
	
	Long countByAdeEventId(Long id);
	
	Long countByAdeEventIdAndContext(Long id, Context ctx);
	
	Long countByNomSessionEpreuveAndDateExamenAndHeureEpreuveAndFinEpreuve(String nomSessionEpreuve, Date dateExamen, Date heureDebut, Date heureFin);
	
	List<SessionEpreuve> findByAdeEventId(Long id);
	
	List<SessionEpreuve> findByAdeEventIdAndContext(Long id, Context ctx);
	
	List<SessionEpreuve> findByIdIn(List<Long> id);
	
	List<SessionEpreuve> findSessionEpreuveByStatutSessionKeyInOrderByDateExamen(List<String> keys);
	
	List<SessionEpreuve>  findSessionEpreuveByContext(Context context);
	
	List<SessionEpreuve>  findByContextAndDateExamenGreaterThanEqual(Context context, Date today);
	
	List<SessionEpreuve>  findByContextAndDateExamenLessThanAndStatutSessionKeyNotIn(Context context, Date today, List<String> keys);
	
	List<SessionEpreuve> findByContextAndDateExamenAndStatutSessionKey(Context context, Date today, String key);
	
	List<SessionEpreuve>  findByContextAndDateExamenAndStatutSessionKeyNotIn(Context context, Date today, List<String> keys);
	
	List<SessionEpreuve>  findByContextAndDateExamenGreaterThanEqualAndDateExamenLessThanEqual(Context context, Date today, Date endDate);
	
	List<SessionEpreuve> findAllByDateExamenLessThanEqualAndDateFinGreaterThanEqualOrDateExamenGreaterThanEqualAndDateExamenLessThanEqualOrDateFinGreaterThanEqualAndDateFinLessThanEqual(Date startDate1, Date endDate1, Date startDate, Date endDate, Date startDateFin, Date endDateFin);
	
	Long countByDateExamenGreaterThanEqual(Date date);
	
	List<SessionEpreuve> findAllByDateExamenGreaterThan(Date date);
	
	List<SessionEpreuve> findByDateExamenLessThanAndDateFinIsNullAndStatutSessionKeyNotOrDateFinLessThanAndStatutSessionKeyNot(Date date, String statut, Date date2, String statut2);
	
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
	
	@Query(value = "select * from session_epreuve, context "
			+ "WHERE session_epreuve.context_id = context.id AND "
			+ "((date_examen >= :startDate and date_examen <= :endDate) or "
			+ " (date_fin >= :startDate and date_fin <= :endDate) or "
			+ "(date_examen <= :startDate and date_fin >= :endDate)) AND context.is_actif = true" , nativeQuery = true)
	List<SessionEpreuve> getAllSessionEpreuveForCalendar(Date startDate, Date endDate);
	
	@Query(value = "SELECT * FROM session_epreuve WHERE "
            + "((date_examen >= :startDate AND date_examen <= :endDate) OR "
            + " (date_fin >= :startDate AND date_fin <= :endDate) OR "
            + " (date_examen <= :startDate AND date_fin >= :endDate)) "
            + "AND context_id = :ctxId", 
       nativeQuery = true)
	List<SessionEpreuve> getAllSessionEpreuveForCalendarByContext(Date startDate, Date endDate, Long ctxId);
	
	@Query(value = "SELECT * FROM session_epreuve WHERE "
            + "((date_examen >= :startDate AND date_examen <= :endDate) OR "
            + " (date_fin >= :startDate AND date_fin <= :endDate) OR "
            + " (date_examen <= :startDate AND date_fin >= :endDate)) OR "
            + " (date_examen >= :startDate AND date_examen <= :endDate AND date_fin IS NULL) "
            + "AND context_id = :ctxId ORDER BY nom_session_epreuve", 
       nativeQuery = true)
	List<SessionEpreuve> getAllSessionEpreuveForAssiduiteByContext(Date startDate, Date endDate, Long ctxId);
	
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
	
	@Query(value = "select distinct annee_univ from session_epreuve where context_id = :ctxId order by annee_univ desc ", nativeQuery = true)
	List<String> findDistinctAnneeUnivOrderByDesc(Long ctxId);
	
	@Query(value = "select distinct type_session.id,libelle from session_epreuve, type_session where  session_epreuve.type_session_id = type_session.id and session_epreuve.context_id = :ctxId", nativeQuery = true)
	List<Object[]> findDistinctTypeSession(Long ctxId);
	
	@Query(value = "select distinct annee_univ from session_epreuve order by annee_univ", nativeQuery = true)
	List<String> findDistinctAnneeUnivAll();
		
	//STATS
	@Query(value = "select site, count(*) as count from session_epreuve, campus, statut_session where "
			+ "session_epreuve.campus_id=campus.id and session_epreuve.context_id=:context "
			+ "and session_epreuve.statut_session_id = statut_session.id "
			+ "and key IN ('CLOSED', 'ENDED') and annee_univ like :anneeUniv group by site order by count desc;", nativeQuery = true)
	List<Object[]> countSessionEpreuveByCampus(Long context, String anneeUniv);
	
	@Query(value = "SELECT CAST(DATE_PART('month', date_examen) AS INTEGER) AS month, count(*) "
			+ "AS count FROM session_epreuve, statut_session  "
			+ "WHERE session_epreuve.context_id=:context "
			+ "and session_epreuve.statut_session_id = statut_session.id "
			+ "AND statut_session.key IN ('CLOSED', 'ENDED') and annee_univ like :anneeUniv GROUP BY month", nativeQuery = true)
	List<Object[]> countSessionEpreuveByYearMonth(Long context, String anneeUniv);
	
	@Query(value = "select statut_session.key as session_key, " +
            "       CASE WHEN statut_session.key IN ('CLOSED', 'ENDED') THEN 'Fermée' ELSE 'Ouverte' END AS statut, " +
            "       count(*) as cnt " +
            "from session_epreuve, context, statut_session " +
            "where session_epreuve.context_id = context.id " +
            "and session_epreuve.statut_session_id = statut_session.id " +
            "and annee_univ like :anneeUniv " +
            "group by statut_session.key, statut " +
            "order by statut_session.key, statut, cnt",
    nativeQuery = true)
	List<Object[]> countAllSessionEpreuvesByContext(String anneeUniv);
	
	@Query(value = "select type_session.key, count(*) from session_epreuve, type_session, statut_session "
			+ "where session_epreuve.type_session_id = type_session.id "
			+ "and session_epreuve.statut_session_id = statut_session.id "
			+ "and session_epreuve.context_id=:context and statut_session.key IN ('CLOSED', 'ENDED') "
			+ "and annee_univ like :anneeUniv group by type_session.key", nativeQuery = true)
	List<Object[]> countSessionEpreuveByType(Long context, String anneeUniv);
	
	@Query(value = "select context.key as ctx, type_session.key as type, count(*) from session_epreuve, context, type_session, statut_session "
			+ "where session_epreuve.type_session_id = type_session.id and "
			+ "session_epreuve.statut_session_id = statut_session.id and "
			+ "session_epreuve.context_id=context.id and statut_session.key IN ('CLOSED', 'ENDED') and annee_univ like :anneeUniv group by context.key, type_session.key order by ctx, type, count;", nativeQuery = true)
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
	
	@Query(value = "select * from session_epreuve, statut_session, type_session "
			+ "where session_epreuve.type_session_id = type_session.id and session_epreuve.context_id =  :contextId and "
			+ "session_epreuve.statut_session_id = statut_session.id and "
			+ "statut_session.key NOT LIKE 'CANCELLED' and session_epreuve.id in "
			+ "(SELECT session_epreuve_id FROM tag_check where session_epreuve.context_id = :contextId GROUP BY session_epreuve_id "
			+ "HAVING COUNT(tag_date) = 0 OR COUNT(*) = SUM(CASE WHEN tag_date IS NULL THEN 1 ELSE 0 END)) "
			+ "and ((DATE(date_examen) < DATE(:date) AND date_fin IS NULL) OR (date_fin IS NOT NULL AND DATE(date_fin) < DATE(:date)))", nativeQuery = true)
	List<SessionEpreuve> findSessionEpreuveWithNoTagDate(Date date, Long contextId);
}
