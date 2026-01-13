package org.esupportail.emargement.repositories.custom;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.springframework.stereotype.Repository;

@Repository
public class TagCheckerRepositoryCustom{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public List<TagChecker> findTagCheckerByUserAppIn(List<UserApp> userApps) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TagChecker> query = criteriaBuilder.createQuery(TagChecker.class);
        Root<TagChecker> c = query.from(TagChecker.class);
        In<UserApp> inClause = criteriaBuilder.in(c.get("userApp"));
        if(!userApps.isEmpty()) {
        	for (UserApp ua : userApps) {
        	    inClause.value(ua);
        	}
        }
        
        query.select(c).where(inClause);
        return entityManager.createQuery(query).getResultList();
	}
	
	public List<TagChecker> findAll(String searchString){
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TagChecker> query = criteriaBuilder.createQuery(TagChecker.class);
        Root<TagChecker> c = query.from(TagChecker.class);
        Join<TagChecker, UserApp> u = c.join("userApp");
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        if(searchString!=null) {
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u.get("eppn")),'%' + searchString.toLowerCase()  + '%'));
        }
        query.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.orderBy(criteriaBuilder.desc(u.get("eppn")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}
}
