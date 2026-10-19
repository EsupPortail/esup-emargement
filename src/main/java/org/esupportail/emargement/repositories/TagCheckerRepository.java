package org.esupportail.emargement.repositories;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagCheckerRepository extends JpaRepository<TagChecker, Long>{
	
	Long countBySessionLocationAndUserApp(SessionLocation sl, UserApp userApp);
	
	List<TagChecker> findBySessionLocation(SessionLocation sl);
	
	List<TagChecker> findTagCheckerBySessionLocationSessionEpreuveId(Long id);
	
	List<TagChecker> findTagCheckerBySessionLocationSessionEpreuveIdAndUserAppEppn(Long id, String eppn);
	
	Page<TagChecker> findTagCheckerBySessionLocationIn(List<SessionLocation> sessionLocations, Pageable pageable);
	
	List<TagChecker> findTagCheckerByContext(Context context);
	
	TagChecker findBySessionLocationAndUserAppEppnEquals(SessionLocation sl, String eppn);
	
	List<TagChecker> findBySessionLocationIdAndUserAppEppn(Long id, String eppn);
	
	Page<TagChecker> findTagCheckerByUserAppEppnEquals(String eppn, Pageable pageable);
	
	boolean existsByUserAppEppn(String eppn);
	
	Optional<TagChecker> findFirstByUserAppEppn(String eppn);
	
	Optional<TagChecker> findFirstByUserAppEppnAndSessionLocationId(String eppn, Long id);

	List<TagChecker> findByContextAndUserAppEppn(Context ctx, String eppn);
	
	Page<TagChecker> findByUserAppEppnAndSessionLocationSessionEpreuveStatutSessionKeyAndSessionLocationSessionEpreuveAnneeUniv(String eppn, String key, String anneeuniv, Pageable pageable);
	
	Optional<TagChecker> findFirstByContextAndUserAppEppn(Context ctx, String eppn);
	
	List<TagChecker> findByUserAppEppnAndSessionLocationSessionEpreuveDateExamenLessThanEqualAndSessionLocationSessionEpreuveDateFinGreaterThanEqualOrUserAppEppnAndSessionLocationSessionEpreuveDateExamenGreaterThanEqualAndSessionLocationSessionEpreuveDateExamenLessThanEqualOrUserAppEppnAndSessionLocationSessionEpreuveDateFinGreaterThanEqualAndSessionLocationSessionEpreuveDateFinLessThanEqual(String eppn, Date startDate, Date endDate, 
			String eppn1, Date startDate1, Date endDate1, String eppn2, Date startDate2, Date endDate2);
	
	List<TagChecker> findByTagDateIsNullAndSessionLocationSessionEpreuveDateExamenBetween(Date startDate, Date endDate);
	
	List<TagChecker> findByTagDateIsNotNullAndSessionLocationSessionEpreuveDateExamenBetween(Date startDate, Date endDate);
	
	List<TagChecker> findBySessionLocationSessionEpreuveDateExamenBetween(Date startDate, Date endDate);
	
	TagChecker findByUserAppAndSessionLocation(UserApp userApp, SessionLocation sessionLocation);
	
	List<TagChecker> findBySessionLocationSessionEpreuveIn(List<SessionEpreuve> ses);
	
	//STATS
	
	@Query(value = "SELECT tc.* "
	        + "FROM tag_checker tc "
	        + "JOIN session_location sl "
	        + "  ON sl.id = tc.session_location_id "
	        + "WHERE sl.session_epreuve_id IN (:sessionIds)",
	        nativeQuery = true)
	List<TagChecker> findBySessionEpreuveIn(
	        @Param("sessionIds") List<Long> sessionIds);

}
