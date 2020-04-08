package org.esupportail.emargement.repositories.custom;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.Person;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepositoryCustom{
	
	@PersistenceContext	
	private EntityManager entityManager;
	
	public List<Person> findByEppn(String eppn) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> query = criteriaBuilder.createQuery(Person.class);
        Root<Person> c = query.from(Person.class);
        query.where(criteriaBuilder.equal(criteriaBuilder.lower(c.get("eppn")),eppn.toLowerCase()));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}
}
