package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.AdeBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdeBranchRepository extends JpaRepository<AdeBranch, Long>{
	
	List <AdeBranch> findByOrderByFullPathAsc();
	
	List <AdeBranch> findByFullPath(String fullPath);

}
