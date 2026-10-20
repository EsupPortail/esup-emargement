package org.esupportail.emargement.repositories;

import java.util.List;
import java.util.Optional;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionLocationRepository extends JpaRepository<SessionLocation, Long>{
 
	Long countBySessionEpreuveId(Long id);
	
	List<SessionLocation> findByLocationIdAndCapaciteGreaterThan(Long id, int cpacite);
	
	Page<SessionLocation> findSessionLocationBySessionEpreuve(SessionEpreuve sessionEpreuve, Pageable pageable);
	
	List<SessionLocation> findSessionLocationBySessionEpreuve(SessionEpreuve sessionEpreuve);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveIdOrderByIsTiersTempsOnlyAscPrioriteAscCapaciteDesc(Long id);
	
	List<SessionLocation> findBySessionEpreuveIdOrderByPriorite(Long id);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyTrueOrderByPriorite(Long id);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveIdAndIsTiersTempsOnlyFalseOrderByPriorite(Long id);
	
	List<SessionLocation> findSessionLocationByLocationIdIn(List<Long> ids);
	
	List<SessionLocation> findSessionLocationByContext(Context context);
	
	//List<SessionLocation> findByContextAndId(Context context, Long id);
	
	Optional<SessionLocation> findFirstByContextAndId(Context context, Long id);
	
	List<SessionLocation> findSessionLocationBySessionEpreuveId(Long Id);
	
	SessionLocation findSessionLocationBySessionEpreuveIdAndLocationNom(Long id, String nom);

	SessionLocation findBySessionEpreuveAndLocationAndContext(SessionEpreuve sessionEpreuve, Location location,
			Context context);
	
}
