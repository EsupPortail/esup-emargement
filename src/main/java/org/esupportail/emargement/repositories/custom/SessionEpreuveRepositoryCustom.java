package org.esupportail.emargement.repositories.custom;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
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
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(c.get("nomSessionEpreuve")),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u.get("site")),'%' + searchString.toLowerCase()  + '%'));
        }
        query.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.orderBy(criteriaBuilder.desc(c.get("dateExamen")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}
	
	public Specification<SessionEpreuve> getSpecFromDatesAndExample(
			Date from, Date to, Example<SessionEpreuve> example) {
		
		return (Specification<SessionEpreuve>) (root, query, builder) -> {
			final List<Predicate> predicates = new ArrayList<>();
			final List<Predicate> predicates2 = new ArrayList<>();
			
			Expression<String> exprDateExamen = builder.function("to_char", String.class,
					root.get("dateExamen"), builder.literal("yyyy-MM-dd"));
			
			Expression<String> exprDatefin = builder.function("to_char", String.class,
					root.get("dateFin"), builder.literal("yyyy-MM-dd"));
			
			Format formatter = new SimpleDateFormat("yyyy-MM-dd");

			if (from != null) {
				predicates.add(builder.greaterThanOrEqualTo(exprDateExamen, formatter.format(from)));
				predicates2.add(builder.greaterThanOrEqualTo(exprDatefin, formatter.format(from)));
			}
			if (to != null) {
				predicates.add(builder.lessThanOrEqualTo(exprDateExamen, formatter.format(to)));
				predicates2.add(builder.lessThanOrEqualTo(exprDatefin, formatter.format(to)));
			}
			predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));
			predicates2.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));
			
			Predicate finalPredicate
			  = builder
			  .or(builder.and(predicates.toArray(new Predicate[predicates.size()])), builder.and(predicates2.toArray(new Predicate[predicates2.size()])));

			return finalPredicate;
		};
	}
}
