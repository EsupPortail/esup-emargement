package org.esupportail.emargement.repositories;

import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TagCheckRepository extends JpaRepository<TagCheck, Long>{
	
	Long countBySessionEpreuveId(Long id);
	
	Page<TagCheck> findTagCheckBySessionEpreuveId(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdOrderByPersonEppn(Long id, Pageable pageable);
	
	List<TagCheck> findTagCheckBySessionEpreuveAnneeUniv(String anneeUniv);
	
	List<TagCheck> findTagCheckBySessionEpreuveId(Long id);
	
	List<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullOrderByPersonEppn(Long id);
	
	Page<TagCheck> findTagCheckByPersonEppn(String eppn, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndIsTiersTempsTrueOrderByPersonEppn(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndIsTiersTempsFalseOrderByPersonEppn(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndPersonEppnEquals(Long id, String eppn, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsOrderByPersonEppn(Long id,  Long slId, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEquals(Long id, String eppn,  Long slId, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsTrue(Long id, String eppn,Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsFalse(Long id, String eppn, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(Long id, String eppn,  Long slId, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrueOrderByPersonEppn(Long id, Long slId, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalseOrderByPersonEppn(Long id, Long slId, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(Long id, String eppn, Long slId, Pageable pageable);
	
	List<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(Long id);
	
	List<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(Long id);
	
	Long countBySessionLocationExpected(SessionLocation sl);
	
	Long countBySessionLocationExpectedId(Long id);
	
	Long countBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedId(Long id, Long id2);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNull(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNull(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndSessionLocationBadgedIsNotNull(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndDateEnvoiConvocationIsNull(Long id);
	
	Long countBySessionLocationExpectedIdAndTagDateIsNotNull(Long id);
	
	Long countBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(Long id);
	
	Long countBySessionEpreuveIdAndTagDateIsNotNull(Long id);
	
	Long countBySessionEpreuveIdAndTagDateIsNotNullAndSessionLocationExpectedIsNotNull(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNull(Long id);
	
	Long countBySessionEpreuveIdAndPersonEppnEquals(Long id, String eppn);

	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsTrue(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNullAndIsTiersTempsFalse(Long id);
	
	Long countTagCheckBySessionLocationBadgedIdAndPersonEppnEquals(Long id, String eppn);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedIdOrderByTagDate(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedIdAndPersonEppnEqualsOrSessionLocationBadgedIdAndPersonEppnEquals(Long id, String eppn, Long id1, String eppn1, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(Long id, String eppn, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedIdOrSessionLocationBadgedIdAndPersonEppnEquals(Long id, Long otherId, String eppn, Pageable pageable);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrue(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalse(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsTrueAndSessionLocationExpectedId(Long id, Long sid);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedIsNotNullAndIsTiersTempsFalseAndSessionLocationExpectedId(Long id, Long sid);
	
	List<TagCheck> findTagCheckBySessionLocationExpectedIdAndPersonEppnEquals(Long id, String eppn);
	
	Long countTagCheckBySessionEpreuveIdAndPersonEppnEquals(Long id, String eppn);
	
	Long countTagCheckBySessionEpreuveId(Long Id);
	
	Long countTagCheckBySessionEpreuveIdAndTagDateIsNotNull(Long Id);
	
	Long countTagCheckBySessionEpreuveIdAndTagDateIsNotNullAndSessionLocationExpectedLocationIdIsNotNull(Long Id);
	
	Long countTagCheckBySessionEpreuveIdAndIsTiersTempsTrue(Long Id);
	
	Long countTagCheckBySessionEpreuveIdAndIsTiersTempsTrueAndTagDateIsNotNull(Long Id);
	
	Long countTagCheckBySessionEpreuveIdAndIsTiersTempsFalse(Long Id);
	
	Long countTagCheckBySessionEpreuveIdAndIsTiersTempsFalseAndTagDateIsNotNull(Long Id);
	
	List<TagCheck> findTagCheckBySessionLocationExpectedId(Long id);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedId(Long id, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedIdOrSessionLocationExpectedIsNullAndSessionLocationBadgedId(Long id, Long badgedId, Pageable pageable);
	
	Page<TagCheck> findTagCheckBySessionLocationExpectedIsNullAndSessionLocationBadgedId(Long id, Pageable pageable);
	
	Long  countTagCheckBySessionLocationExpectedIdIsNullAndSessionLocationBadgedId(Long id);
	
	List<TagCheck> findTagCheckBySessionLocationExpectedIdOrderByPersonEppn(Long id);
	
	List<TagCheck> findTagCheckBySessionEpreuveIdAndSessionLocationExpectedIdOrderByPersonEppn(Long id, Long slbId);
	
	List<TagCheck> findTagCheckByContext(Context context);
	
	Long  countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsTrue(Long id, String eppn);
	
	Long  countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(Long id, String eppn, Long repartitionId);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsTrue(Long id, Long repartitionId);
	
	Long countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndIsTiersTempsFalse(Long id, String eppn);
	
	Long countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(Long id, String eppn, Long repartitionId);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEqualsAndIsTiersTempsFalse(Long id, Long repartitionId);
	
	Long countTagCheckBySessionEpreuveIdAndPersonEppnEqualsAndSessionLocationExpectedLocationIdEquals(Long id, String eppn, Long repartitionId);
	
	Long countTagCheckBySessionEpreuveIdAndSessionLocationExpectedLocationIdEquals(Long id, Long repartitionId);
	
	Long countTagCheckBySessionLocationExpected(SessionLocation sl);
	
	Long countTagCheckBySessionLocationExpectedAndSessionLocationBadgedIsNotNull(SessionLocation sl);
	
	Long countTagCheckBySessionEpreuveIdAndIsCheckedByCardTrue(Long id);
	
	Long countTagCheckBySessionEpreuveIdAndIsCheckedByCardFalse(Long id);
	
	Long countTagCheckByGroupeId(Long id);
	
	List<TagCheck> findTagCheckByGroupeId(Long id);
	
	Long  countTagCheckByPerson(Person person);
	
	Long  countTagCheckBySessionEpreuveIdAndProxyPersonIsNotNull(Long id);
	
	@Query(value = "select count(*) from tag_check  where session_epreuve_id in (select id from session_epreuve where annee_univ = :anneeUniv and context_id = :ctxId ) and person_id= :personId", nativeQuery = true)
	Long countAnonymousTagCheckBySAnneeUnivAndContextId(String anneeUniv, Long ctxId, Long personId);
	
	@Query(value = "select count(*) from tag_check  where session_epreuve_id in (select id from session_epreuve where annee_univ = :anneeUniv and context_id = :ctxId)", nativeQuery = true)
	Long countTagCheckByAnneeUnivAndContextId(String anneeUniv, Long ctxId);
	
	@Query(value = "select count(*) from tag_check, session_location, person, location, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_location.id = tag_check.session_location_expected_id "
			+ "and location.id= session_location.location_id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and location.nom= :nomLocation and person.eppn= :eppn and date_examen= :date and session_epreuve.nom_session_epreuve = :nom", nativeQuery = true)
	Long checkIsTagable(String nomLocation, String eppn, Date date, String nom);
	
	@Query(value = "select session_location.id from tag_check, session_location, person, location, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_location.id = tag_check.session_location_expected_id "
			+ "and location.id= session_location.location_id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and location.nom= :nomLocation and person.eppn= :eppn and date_examen= :date and session_epreuve.nom_session_epreuve = :nom", nativeQuery = true)
	Long getSessionLocationId(String nomLocation, String eppn, Date date, String nom);
	
	@Query(value = "select context.key from tag_check, session_location, person, location, session_epreuve, context "
			+ "where tag_check.person_id = person.id "
			+ "and session_location.id = tag_check.session_location_expected_id "
			+ "and location.id= session_location.location_id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and context.id = session_location.context_id "
			+ "and location.nom= :nomLocation and person.eppn= :eppn and date_examen= :date and session_epreuve.nom_session_epreuve = :nom", nativeQuery = true)
	String getContextId(String nomLocation, String eppn, Date date, String nom);
	
	@Query(value = "select context.key from tag_check, session_location, person, location, session_epreuve, context "
			+ "where tag_check.person_id = person.id "
			+ "and session_location.id = tag_check.session_location_expected_id "
			+ "and location.id= session_location.location_id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and context.id = session_location.context_id "
			+ "and session_epreuve.id = :seId and person.eppn= :eppn and date_examen= :date", nativeQuery = true)
	String getContextIdBySeId(Long seId, String eppn, Date date);
	
	@Query(value = "select session_location.id from tag_check, session_location, person, location, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_location.id = tag_check.session_location_expected_id "
			+ "and location.id= session_location.location_id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and location.nom= :nomLocation and date_examen= :date and session_epreuve.nom_session_epreuve = :nom LIMIT 1", nativeQuery = true)
	Long getSessionLocationId(String nomLocation, Date date, String nom);
	
	
	@Query(value = "select session_location.id from tag_check, session_location, person, location, session_epreuve "
			+ "where tag_check.person_id = person.id "
			+ "and session_location.id = tag_check.session_location_expected_id "
			+ "and location.id= session_location.location_id "
			+ "and session_epreuve.id = tag_check.session_epreuve_id "
			+ "and person.eppn= :eppn and date_examen= :date and session_epreuve.nom_session_epreuve = :nom", nativeQuery = true)
	Long getSessionLocationIdExpected(String eppn, Date date, String nom);
	
	@Query(value = "select * from tag_check, person where tag_check.person_id = person.id and person.eppn= :eppn and session_location_expected_id= :id", nativeQuery = true)
	TagCheck findTagCheckBySessionLocationExpectedIdAndEppn(Long id, String eppn);
	
	@Query(value = "select * from tag_check, person where tag_check.person_id = person.id and person.eppn= :eppn and session_epreuve_id= :id", nativeQuery = true)
	TagCheck findTagCheckBySessionEpreuveIdAndEppn(Long id, String eppn);
	
	
	//STATS
	@Query(value = "select (CASE WHEN session_location_badged_id IS NOT NULL THEN 'Present' WHEN session_location_badged_id IS NULL THEN 'Absent' END) as tagcheck,  count(*) "
			+ "from tag_check, session_epreuve where tag_check.session_epreuve_id = session_epreuve.id AND tag_check.context_id = :context and session_location_expected_id is not null "
			+ "AND is_session_epreuve_closed = 't' and annee_univ like :anneeUniv group by tagcheck order by count desc", nativeQuery = true)
	List<Object[]> countPresenceByContext(Long context, String anneeUniv);
	
	@Query(value = "SELECT CAST(DATE_PART('month', tag_date) AS INTEGER) AS month, count(*) AS count FROM tag_check, session_epreuve "
			+ "WHERE tag_check.session_epreuve_id = session_epreuve.id AND tag_date is not null AND tag_check.context_id=:context AND is_session_epreuve_closed = 't' and annee_univ like :anneeUniv GROUP BY month", nativeQuery = true)
	List<Object[]> countTagCheckByYearMonth(Long context, String anneeUniv);
	
	@Query(value = "select key, CASE WHEN tag_date is null THEN 'Absent' ELSE 'Présent' END AS statut, count(*) as count  from tag_check, session_epreuve, context where  "
			+ "tag_check.context_id=context.id and  tag_check.session_epreuve_id=session_epreuve.id "
			+ "and is_session_epreuve_closed='t' and annee_univ like :anneeUniv group by key, statut order by key, statut, count desc", nativeQuery = true)
	List<Object[]> countTagChecksByContext(String anneeUniv);
	
	@Query(value = "SELECT to_char(date_trunc('minute',  tag_date), 'HH24:MI') as timeTag, count(*) as count FROM tag_check where session_epreuve_id = :seId and tag_date is not null "
			+ "and is_checked_by_card='t' GROUP BY timeTag order by timeTag", nativeQuery = true)
	List<Object[]> countTagChecksByTimeBadgeage(Long seId);
	
	@Query(value = "select CASE WHEN is_checked_by_card ='t' THEN 'Par carte' ELSE 'Manuellement' END AS type, count(*) as count from tag_check, context, session_epreuve  where "
			+ "tag_check.context_id=context.id and  tag_check.session_epreuve_id=session_epreuve.id "
			+ "AND tag_check.context_id=:context and is_checked_by_card is not null and  is_session_epreuve_closed = 't' and annee_univ like :anneeUniv group by is_checked_by_card", nativeQuery = true)
	List<Object[]> countTagChecksByTypeBadgeage(Long context, String anneeUniv);
	
	@Query(value = "select event_count, count(*) as users_count  from (select count(eppn)  as event_count from tag_check, person, session_epreuve where tag_check.person_id = person.id "
			+ "and tag_check.session_epreuve_id=session_epreuve.id and tag_check.context_id = :context and session_location_badged_id is not null and is_session_epreuve_closed = 't' "
			+ "and annee_univ like :anneeUniv  group by eppn) t group by event_count", nativeQuery = true)
	List<Object[]> countTagCheckBySessionLocationBadgedAndPerson(Long context, String anneeUniv);
}

