package org.esupportail.emargement.repositories;

import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TagCheckerRepository extends JpaRepository<TagChecker, Long>{
	
	Long countBySessionLocationAndUserApp(SessionLocation sl, UserApp userApp);
	
	List<TagChecker> findBySessionLocation(SessionLocation sl);
	
	List<TagChecker> findBySessionLocationId(Long id);
	
	List<TagChecker> findTagCheckerBySessionLocationSessionEpreuveId(Long id);
	
	List<TagChecker> findTagCheckerBySessionLocationSessionEpreuveIdAndUserApp(Long id, UserApp userApp);
	
	List<TagChecker> findTagCheckerBySessionLocationSessionEpreuveIdAndUserAppEppn(Long id, String eppn);
	
	Page<TagChecker> findTagCheckerBySessionLocationIn(List<SessionLocation> sessionLocations, Pageable pageable);
	
	List<TagChecker> findTagCheckerByContext(Context context);
	
	TagChecker findBySessionLocationAndUserAppEppnEquals(SessionLocation sl, String eppn);
	
	List<TagChecker> findBySessionLocationIdAndUserAppEppn(Long id, String eppn);
	
	Page<TagChecker> findTagCheckerByUserAppEppnEquals(String eppn, Pageable pageable);
	
	List<TagChecker> findByContextAndUserAppEppn(Context ctx, String eppn);
	
	List<TagChecker> findByUserAppEppnAndSessionLocationSessionEpreuveDateExamenLessThanEqualAndSessionLocationSessionEpreuveDateFinGreaterThanEqualOrUserAppEppnAndSessionLocationSessionEpreuveDateExamenGreaterThanEqualAndSessionLocationSessionEpreuveDateExamenLessThanEqualOrUserAppEppnAndSessionLocationSessionEpreuveDateFinGreaterThanEqualAndSessionLocationSessionEpreuveDateFinLessThanEqual(String eppn, Date startDate, Date endDate, 
			String eppn1, Date startDate1, Date endDate1, String eppn2, Date startDate2, Date endDate2);
	
	//STATS
	@Query(value = "select user_app.eppn, count(*) from tag_checker, user_app, session_location, statut_session, session_epreuve "
			+ "where tag_checker.user_app_id = user_app.id "
			+ "AND session_location.session_epreuve_id = session_epreuve.id " 
			+ "AND session_epreuve.statut_session_id = statut_session.id " 
			+ "AND tag_checker.session_location_id=session_location.id  and session_epreuve_id in (select id from session_epreuve "
			+ "where statut_session.key IN ('CLOSED', 'ENDED') and annee_univ like :anneeUniv) "
			+ "AND tag_checker.context_id = :context group by user_app.eppn order by count desc", nativeQuery = true)
	List<Object[]> countTagCheckersByContext(Long context, String anneeUniv);

}
