package org.esupportail.emargement.repositories.custom;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.AssiduiteBean2;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheck.Motif;
import org.esupportail.emargement.services.TagCheckService;
import org.springframework.stereotype.Repository;

@Repository
public class TagCheckRepositoryCustom{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Resource
	TagCheckService tagCheckService;
	
	public List<TagCheck> findAll(String searchString, Long sessionEpreuveId) {
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TagCheck> query = criteriaBuilder.createQuery(TagCheck.class);
        Root<TagCheck> c = query.from(TagCheck.class);
        Join<TagCheck, Person> u2 = c.join("person");
        List<Predicate> orPredicates = new ArrayList<>();
        List<Predicate> predicates = new ArrayList<>();
        if(searchString!=null) {
        	if(sessionEpreuveId != null) {
        		predicates.add(criteriaBuilder.equal(c.get("sessionEpreuve"),sessionEpreuveId));
        	}
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u2.get("eppn")),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u2.get("numIdentifiant")),'%' + searchString.toLowerCase()  + '%'));
        }
        predicates.add(criteriaBuilder.or(orPredicates.toArray(new Predicate[] {})));
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(criteriaBuilder.asc(u2.get("eppn")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();

	}
	
	
	public List<TagCheck> findAll2(String searchString, Long sessionEpreuveId) {
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TagCheck> query = criteriaBuilder.createQuery(TagCheck.class);
        Root<TagCheck> c = query.from(TagCheck.class);
        Join<TagCheck, Guest> u2 = c.join("guest");
        List<Predicate> orPredicates = new ArrayList<>();
        List<Predicate> predicates = new ArrayList<>();
        if(searchString!=null) {
        	if(sessionEpreuveId != null) {
        		predicates.add(criteriaBuilder.equal(c.get("sessionEpreuve"),sessionEpreuveId));
        	}
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u2.get("email")),'%' + searchString.toLowerCase()  + '%'));
        	orPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(u2.get("nom")),'%' + searchString.toLowerCase()  + '%'));
        }
        predicates.add(criteriaBuilder.or(orPredicates.toArray(new Predicate[] {})));
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(criteriaBuilder.asc(u2.get("nom")));
        query.select(c);
        return entityManager.createQuery(query).getResultList();

	}
	
	public List<TagCheck> findForAssiduite(AssiduiteBean2 assiduiteBean) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TagCheck> query = criteriaBuilder.createQuery(TagCheck.class);
        Root<TagCheck> c = query.from(TagCheck.class);
        Join<TagCheck, Person> u2 = c.join("person");
        Join<Person, Groupe> g1 = u2.join("groupes");
        List<Predicate> orPredicates = new ArrayList<>();
        List<Predicate> predicates = new ArrayList<>();
        //dates , type, motif, etudiant
        List<TagCheck> tcs = new ArrayList<>();
        if(assiduiteBean!=null) {
        	if(assiduiteBean.getMotif()!=null && !assiduiteBean.getMotif().isEmpty()) {
        		orPredicates.add(criteriaBuilder.equal(c.get("absence"),Motif.valueOf(assiduiteBean.getMotif())));
        	}
        	if(assiduiteBean.getCodeEtu()!=null && !assiduiteBean.getCodeEtu().isEmpty()) {
        		orPredicates.add(criteriaBuilder.like(u2.get("numIdentifiant"), '%' + assiduiteBean.getCodeEtu() + '%'));
        	}
    /*    	if(assiduiteBean.getGroupe()!=null && !assiduiteBean.getGroupe().isEmpty()) {
        		orPredicates.add(criteriaBuilder.like(g1.get("nom"), '%' + assiduiteBean.getGroupe() + '%'));
        	}*/
        	if(assiduiteBean.getNom()!=null && !assiduiteBean.getNom().isEmpty()) {
        		orPredicates.add(criteriaBuilder.like(u2.get("eppn"), '%' + assiduiteBean.getNom() + '%'));
        	}
        	predicates.add(criteriaBuilder.and(orPredicates.toArray(new Predicate[] {})));
            query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
          //  query.orderBy(criteriaBuilder.asc(u2.get("nom")));
            query.select(c);
        }
        tcs = entityManager.createQuery(query).getResultList();
        tagCheckService.setNomPrenomTagChecks(tcs, false, false);
        return tcs;
	}
}
