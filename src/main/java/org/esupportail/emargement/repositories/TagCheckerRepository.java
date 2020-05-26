package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TagCheckerRepository extends JpaRepository<TagChecker, Long>{
	
	Long countBySessionLocationId(Long id);
	
	List<TagChecker> findBySessionLocation(SessionLocation sl);

	List<TagChecker> findTagCheckerBySessionLocationSessionEpreuveId(Long id);

	Page<TagChecker> findTagCheckerBySessionLocationIn(List<SessionLocation> sessionLocations, Pageable pageable);
	
	List<TagChecker> findTagCheckerByContext(Context context);
	
	TagChecker findBySessionLocationAndUserAppEppnEquals(SessionLocation sl, String eppn);
	
	Page<TagChecker> findTagCheckerByUserAppEppnEquals(String eppn, Pageable pageable);
	
	//STATS
	@Query(value = "select eppn, count(*) from tag_checker, user_app, session_location where tag_checker.user_app_id = user_app.id "
			+ "and tag_checker.session_location_id=session_location.id  and session_epreuve_id in (select id from session_epreuve where is_session_epreuve_closed='t') "
			+ "and tag_checker.context_id = :context group by eppn order by count desc", nativeQuery = true)
	List<Object> countTagCheckersByContext(Long context);

	@Query(value = "select key, count(*) as count from tag_checker, context where tag_checker.context_id=context.id and "
			+ "session_location_id is not null group by key order by key, count desc", nativeQuery = true)
	List<Object> countNbTagCheckerByContext();

}
