package org.esupportail.emargement.repositories.custom;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.stereotype.Repository;

@Repository
public class LocationRepositoryCustom{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public List<Location> findAll(String searchString){
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Location> query = criteriaBuilder.createQuery(Location.class);
        Root<Location> c = query.from(Location.class);
        Join<SessionEpreuve, Campus> u = c.join("campus");
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        if(searchString!=null) {
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(c.get("nom")),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(c.get("adresse")),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(c.get("capacite").as(String.class)),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u.get("site")),'%' + searchString.toLowerCase()  + '%'));
        }
        query.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.orderBy(criteriaBuilder.desc(c.get("nom")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}
}
