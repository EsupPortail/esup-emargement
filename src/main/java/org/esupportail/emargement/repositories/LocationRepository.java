package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long>{
	
	Long countByNom(String nom);
	
	Page<Location> findByNom(String nom, Pageable pageable);
	
	List<Location> findLocationByCampus(Campus campus);
	
	List<Location> findLocationByContext(Context context);
	
	//STATS
	@Query(value = "select key, count(*) as count from location, context where location.context_id=context.id group by key order by key, count desc", nativeQuery = true)
	List<Object> countLocationsByContext();
}
