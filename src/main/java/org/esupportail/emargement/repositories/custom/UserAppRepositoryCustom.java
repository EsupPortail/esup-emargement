package org.esupportail.emargement.repositories.custom;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.domain.UserApp.Role;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserAppRepositoryCustom{

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	public List<UserApp> findAll(String searchString){
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserApp> query = criteriaBuilder.createQuery(UserApp.class);
        Root<UserApp> c = query.from(UserApp.class);
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        if(searchString!=null) {
        	Expression<String> lastConnexion = criteriaBuilder.function("to_char", String.class,
        	        c.get("lastConnexion"), criteriaBuilder.literal("dd-MM-yyyy"));

        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(lastConnexion), "%" + searchString.toLowerCase() + "%"));
        	
        	Expression<String> dateCreation = criteriaBuilder.function("to_char", String.class,
        	        c.get("dateCreation"), criteriaBuilder.literal("dd-MM-yyyy"));

        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(dateCreation), "%" + searchString.toLowerCase() + "%"));
        	
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(c.get("eppn")),'%' + searchString.toLowerCase()  + '%'));
        	for (Role role : Role.values()) {
        	    if (role.name().contains(searchString.toUpperCase())) {
        	    	orPredicates.add(criteriaBuilder.like(criteriaBuilder.upper(c.get("userRole").as(String.class)), role.name().toUpperCase()));
        	    }
        	}
        }
        query.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.orderBy(criteriaBuilder.desc(c.get("eppn")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}
	
	//@Transactional
	public List<UserApp> findByEppn(String eppn) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserApp> query = criteriaBuilder.createQuery(UserApp.class);
        Root<UserApp> c = query.from(UserApp.class);
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        if(eppn!=null) {
        	orPredicates.add(criteriaBuilder.equal(criteriaBuilder.lower(c.get("eppn")),eppn.toLowerCase()));
        }
        query.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.select(c);
        return entityManager.createQuery(query).getResultList();
	}

}
