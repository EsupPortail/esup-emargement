package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.Absence;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long>{
	
	List<StoredFile> findByContext(Context context);
	
	List<StoredFile> findBySessionEpreuve(SessionEpreuve se);
	
	List<StoredFile> findByAbsence(Absence absence);
	
	Long countBySessionEpreuve(SessionEpreuve se);
	
	Long countByAbsence(Absence absence);
}
