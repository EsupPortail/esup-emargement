package org.esupportail.emargement.repositories.custom;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.repositories.LogsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class LogsRepositoryCustom{
	
	@Autowired
	LogsRepository logsRepository;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	public Page<Log> findAll(Log log, String stringDate, Pageable pageable) throws ParseException{
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Log> query = criteriaBuilder.createQuery(Log.class);
        Root<Log> c = query.from(Log.class);
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        if(log!=null) {
        	if(!stringDate.isEmpty()) {
            	String[] splitDate = stringDate.split("/");
            	String finalDate = splitDate[2] + "-" + splitDate[1] + "-" + splitDate[0];
        		Expression<String> dateStringExpr = criteriaBuilder.function("to_char", String.class,
            	        c.get("logDate"), criteriaBuilder.literal("yyyy-MM-dd"));
        		
        		orPredicates.add(criteriaBuilder.equal(dateStringExpr,finalDate));
        	}
        	if(log.getAction()!=null && !log.getAction().isEmpty()) {
        		orPredicates.add(criteriaBuilder.equal(c.get("action"), log.getAction()));
        	}
        	if(log.getEppn()!=null && !log.getEppn().isEmpty()) {
        		orPredicates.add(criteriaBuilder.equal(c.get("eppn"), log.getEppn()));
        	}
        	if(log.getType()!=null && !log.getType().isEmpty()) {
        		orPredicates.add(criteriaBuilder.equal(c.get("type"), log.getType()));
        	}
        	if(log.getRetCode()!=null && !log.getRetCode().isEmpty()) {
        		orPredicates.add(criteriaBuilder.equal((c.get("retCode")), log.getRetCode()));
        	}
        	if(log.getCibleLogin()!=null && !log.getCibleLogin().isEmpty()) {
        		orPredicates.add(criteriaBuilder.equal(c.get("cibleLogin"), log.getCibleLogin()));
        	}
        }
        query.where(criteriaBuilder.and(orPredicates.toArray(new Predicate[orPredicates.size()])));
        query.orderBy(criteriaBuilder.desc(c.get("logDate")));
        query.select(c);
        List<Log> list = entityManager.createQuery(query).getResultList();
		Page <Log> page = new PageImpl<Log>(list, pageable, Long.valueOf(list.size()));

        return page;
	}

	public List<String> findDistinctEppn() {
		List<Log> logs = logsRepository.findAll();
		return logs.stream().map(l -> l.getEppn()).distinct().collect(Collectors.toList());
	}
	
	public List<String> findDistinctAction() {
		List<Log> logs = logsRepository.findAll();
		return logs.stream().map(l -> l.getAction()).distinct().collect(Collectors.toList());
	}
	
	public List<String> findDistinctCibleLogin() {
		List<Log> logs = logsRepository.findAll();
		return logs.stream().map(l -> l.getCibleLogin()).distinct().collect(Collectors.toList());
	}

}
