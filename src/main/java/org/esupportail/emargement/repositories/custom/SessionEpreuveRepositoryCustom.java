package org.esupportail.emargement.repositories.custom;

import java.text.ParseException;
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
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.stereotype.Repository;

@Repository
public class SessionEpreuveRepositoryCustom{

	@PersistenceContext
	private EntityManager entityManager;
	
	public List<SessionEpreuve> findAll(String searchString) throws ParseException{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionEpreuve> query = criteriaBuilder.createQuery(SessionEpreuve.class);
        Root<SessionEpreuve> c = query.from(SessionEpreuve.class);
        Join<SessionEpreuve, Campus> u = c.join("campus");
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        if(searchString!=null) {
        	/*Expression<String> dateStringExpr = criteriaBuilder.function("to_char", String.class,
        	        c.get("dateExamen"), criteriaBuilder.literal("dd-MM-yyyy"));

        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(dateStringExpr), "%" + searchString.toLowerCase() + "%"));*/
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(c.get("nomSessionEpreuve")),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u.get("site")),'%' + searchString.toLowerCase()  + '%'));
        }
        query.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.orderBy(criteriaBuilder.desc(c.get("dateExamen")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}
}
