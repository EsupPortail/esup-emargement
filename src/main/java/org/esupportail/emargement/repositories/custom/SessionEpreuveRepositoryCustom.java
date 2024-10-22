package org.esupportail.emargement.repositories.custom;

import java.text.Format;
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
import javax.persistence.criteria.Subquery;

import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class SessionEpreuveRepositoryCustom{

	@PersistenceContext
	private EntityManager entityManager;
	
	public List<SessionEpreuve> findAll(String searchString){
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionEpreuve> query = criteriaBuilder.createQuery(SessionEpreuve.class);
        Root<SessionEpreuve> c = query.from(SessionEpreuve.class);
        Join<SessionEpreuve, Campus> u = c.join("campus");
        List<Predicate> orPredicates = new ArrayList<>();
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
	        Date from, Date to, Example<SessionEpreuve> example, String view, UserApp userApp) {

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
	        Predicate finalPredicate = null;
	        if("mine".equals(view) || "spe".equals(view)) {
		        Subquery<Long> subquery = query.subquery(Long.class);
		        Root<TagChecker> tagCheckerRoot = subquery.from(TagChecker.class);
		        Join<TagChecker, SessionLocation> tagCheckerJoin = tagCheckerRoot.join("sessionLocation");
		        if ("mine".equals(view)) {
		            subquery.select(tagCheckerJoin.get("sessionEpreuve").get("id"))
		                    .where(builder.equal(tagCheckerRoot.get("userApp"), userApp));
		        }else if ("spe".equals(view) && userApp.getSpeciality()!=null) {
		            Join<TagChecker, UserApp> userAppJoin = tagCheckerRoot.join("userApp");
		            subquery.select(tagCheckerJoin.get("sessionEpreuve").get("id"))
		                    .where(builder.like(userAppJoin.get("speciality"), "%" + userApp.getSpeciality() + "%"));
		        }
	            Predicate sessionEpreuveInSubquery = root.get("id").in(subquery);
	            predicates.add(sessionEpreuveInSubquery);
	            predicates2.add(sessionEpreuveInSubquery);
	            finalPredicate = builder.or(
	                builder.and(predicates.toArray(new Predicate[0])),
	                builder.and(predicates2.toArray(new Predicate[0]))
	            );
	        }
	     else {
				finalPredicate = builder.or(builder.and(predicates.toArray(new Predicate[predicates.size()])),
						builder.and(predicates2.toArray(new Predicate[predicates2.size()])));
	        }
	        return finalPredicate;
	    };
	}
}
