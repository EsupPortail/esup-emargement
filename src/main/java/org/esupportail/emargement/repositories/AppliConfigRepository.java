package org.esupportail.emargement.repositories;

import java.util.List;
import java.util.Optional;

import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AppliConfigRepository extends JpaRepository<AppliConfig, Long> {
    
	Long countByKey(String key);
	
	List<AppliConfig> findAppliConfigByKey(String key);
	
	List<AppliConfig> findAppliConfigByContext(Context context);
	
	List<AppliConfig> findAppliConfigByContextAndCategoryIsNull(Context context);
	
	List<AppliConfig> findAppliConfigByKeyAndContext(String key, Context context);
	
	Optional<AppliConfig> findFirstByContextAndKey(Context context, String key);
	
	Long countByContextAndKeyAndCategory(Context context, String key, String category);
	
	List<AppliConfig> findAllByOrderByCategory();
	
	List<AppliConfig> findAllByCategoryOrderByKey(String category);
	
	List<AppliConfig> findByCategoryAndContextKeyOrderByKey(String category, String context);
	
	@Query(value = "select distinct category from appli_config order by category", nativeQuery = true)
	List<String> findDistinctCategory();
	
	@Query(value = "select * from appli_config where key=:key order by category", nativeQuery = true)
	List<AppliConfig> findByKeyForAllContexts(String key);
	
	@Query(value = "select * from appli_config where category=:cat order by key", nativeQuery = true)
	List<AppliConfig> findByCategoryForAllContexts(String cat);
}
