package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Help;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HelpRepository extends JpaRepository<Help, Long>{
	
	@Query(value = "select count(*) from help where key= :key ", nativeQuery = true)
	Long countByKey(String key);
	
	//@Query(value = "select count(*) from help where key= :key ", nativeQuery = true)
	List<Help> findByKey(String key);

}
