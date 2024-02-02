package org.esupportail.emargement.repositories;

import java.util.List;
import java.util.Optional;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long>{
	
    public boolean existsById(short id);
    public boolean existsByNom(String nom);
    public boolean existsByCampus(Campus c);

	Long countByNom(String nom);

    public Optional<Location> findByNom(String nom);

	Page<Location> findByNom(String nom, Pageable pageable);
	
	public List<Location> findByCampus(Campus c);
	
	List<Location> findLocationByCampus(Campus campus);

	List<Location> findByAdeClassRoomIdAndContext(Long id, Context context);

	List<Location> findLocationByContext(Context context);

	Long countByAdeClassRoomId(Long id);

	//STATS
	@Query(value = "select key, count(*) as count from location, context where location.context_id=context.id group by key order by key, count desc", nativeQuery = true)
	List<Object[]> countLocationsByContext();

	@Query(nativeQuery=true,value="SELECT CASE WHEN COUNT(S.id) > 0 THEN true ELSE false END FROM session_epreuve S JOIN session_location L ON S.id = paper_id WHERE location_id = :id AND statut <> 'CLOSED'") 
	public boolean existsSessionByLocation_IdAndStatutNotClosed(Long id);

	@Query(nativeQuery=true,value="SELECT S.* FROM session_epreuve P JOIN session_location L ON P.id = paper_id WHERE location_id = :id AND statut <> 'CLOSED' ORDER BY nom") 
	public List<SessionEpreuve> findSessionByLocation_IdAndStatutNotClosed(Long id);

	@Query(nativeQuery=true,value="SELECT CASE WHEN COUNT(S.id) > 0 THEN true ELSE false END FROM session_epreuve S JOIN session_location L ON S.id = paper_id WHERE location_id = :id AND statut <> 'CLOSED' AND capacite > :capacity") 
	public boolean existsSessionByLocation_IdAndStatutNotClosedAndCapacityGreaterThan(Long id, int capacity);


	@Query(nativeQuery = true, value = "SELECT * FROM location WHERE campus_id = :campus AND id NOT IN (SELECT L.id FROM location L JOIN session_location ON L.id = location_id JOIN session_epreuve P ON paper_id = P.id WHERE P.id = :paper)") 
	public List<Location> findByCampus_IdAndPaper_IdNot(Long campus, int paper);



}
