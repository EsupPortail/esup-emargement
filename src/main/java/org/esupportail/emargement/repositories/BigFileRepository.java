package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.Context;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BigFileRepository extends JpaRepository<BigFile, Long>{
	
	List<BigFile> findByContext(Context context);

}
