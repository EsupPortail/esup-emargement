package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionLocationRepository extends JpaRepository<SessionLocation, Long>{
 
	Long countBySessionEpreuveId(Long id);
	
	List<SessionLocation> findByLocationIdAndCapaciteGreaterThan(Long id, int cpacite);
	
	Page<SessionLocation> findSessionLocationBySessionEpreuve(SessionEpreuve sessionEpreuve, Pageable pageable);
	
	List<SessionLocation> findSessionLocationBySessionEpreuve(SessionEpreuve sessionEpreuve);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveIdOrderByIsTiersTempsOnlyAscPrioriteAscCapaciteDesc(Long id);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyTrueOrderByPriorite(Long id);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyFalseOrderByPriorite(Long id);
	
	List<SessionLocation> findSessionLocationByLocationIdIn(List<Long> ids);
	
	List<SessionLocation> findSessionLocationByContext(Context context);
	
	List<SessionLocation> findByContextAndId(Context context, Long id);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveId(Long Id);
	
	 SessionLocation findSessionLocationBySessionEpreuveIdAndLocationNom(Long id, String nom);
	
	//STATS
	@Query(value = "select nom, count(*) from session_location, location, session_epreuve where session_location.location_id=location.id "
			+ "and session_location.session_epreuve_id = session_epreuve.id and session_location.context_id=:context "
			+ "AND is_session_epreuve_closed = 't' and annee_univ like :anneeUniv group by nom order by count desc", nativeQuery = true)
	List<Object[]> countSessionLocationByLocation(Long context, String anneeUniv);
	
}
