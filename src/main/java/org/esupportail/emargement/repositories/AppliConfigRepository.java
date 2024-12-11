package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.Context;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppliConfigRepository extends JpaRepository<AppliConfig, Long> {
    
	Long countByKey(String key);
	
	List<AppliConfig> findAppliConfigByKey(String key);
	
	List<AppliConfig> findAppliConfigByContext(Context context);
	
	List<AppliConfig> findAppliConfigByContextAndCategoryIsNull(Context context);
	
	List<AppliConfig> findAppliConfigByKeyAndContext(String key, Context context);
	
	List<AppliConfig> findByContextAndKey(Context context, String key);
	
	Long countByContextAndKeyAndCategory(Context context, String key, String category);
	
	List<AppliConfig> findAllByOrderByCategory();
	
	List<AppliConfig> findAllByCategoryOrderByKey(String category);
}
