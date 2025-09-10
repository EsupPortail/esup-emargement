package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.StatutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatutSessionRepository extends JpaRepository<StatutSession, Long>{

	List<StatutSession> findByContext(Context context);
	
	List<StatutSession> findByContextKey(String key);
	
	StatutSession findByKey(String key);
	
	StatutSession findByKeyAndContext(String key, Context context);
}
