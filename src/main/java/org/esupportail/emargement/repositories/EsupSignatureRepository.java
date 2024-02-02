package org.esupportail.emargement.repositories;

import java.util.List;

import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsupSignatureRepository extends JpaRepository<EsupSignature, Long>{

	List<EsupSignature> findBySignRequestId(Long signRequestId);
	
	List<EsupSignature> findByTagCheckIn(List<TagCheck> list);
	
	List<EsupSignature> findByTagCheck(TagCheck tc);
	
	Long countBySessionEpreuve(SessionEpreuve sessionEpreuve);
	
	Page<EsupSignature> findBySessionEpreuve(SessionEpreuve sessionEpreuve, Pageable pageable);
}
