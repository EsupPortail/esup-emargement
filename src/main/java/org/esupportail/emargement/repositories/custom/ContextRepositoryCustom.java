package org.esupportail.emargement.repositories.custom;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

@Repository
public class ContextRepositoryCustom{
	
	@PersistenceContext
	private EntityManager entityManager;

}
