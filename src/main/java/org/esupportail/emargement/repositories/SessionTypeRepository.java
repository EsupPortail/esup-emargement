package org.esupportail.emargement.repositories;
import java.util.List;
import java.util.Optional;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository public interface SessionTypeRepository extends JpaRepository<SessionType, Short> {
	boolean existsByKey(String key);
	boolean existsByKeyIgnoreCase(String key);
	SessionType findByKey(String key);
	Optional<SessionType> findByKeyIgnoreCase(String key);
  List<SessionType> findByContext(Context context);
	List<SessionType> findAllByOrderByTitle();
}
