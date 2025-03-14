package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextRepository extends JpaRepository<Context, Long>{
	
	@Query(value = "select count(*) from context where key= :key ", nativeQuery = true)
	Long countByContextKey(String key);
	
	@Query(value = "select * from context where key = :key Limit 1", nativeQuery = true)
	Context findByContextKey(String key);
	
	@Query(value = "select * from context where id = :id Limit 1", nativeQuery = true)
	Context findByContextId(Long id);
	
	@Query(value = "select context_priority, key from context, user_app where context.id = user_app.context_id and eppn =:eppn ", nativeQuery = true)
	List<Object[]>  findByEppn(String eppn);
	
	@Query(value = "select distinct key from context order by key", nativeQuery = true)
	List<String> findDistinctKey();
	
	List<Context> findByIsActifTrueOrderByKey();
	
	Context findByKey(String key);
}
