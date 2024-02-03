package org.esupportail.emargement.repositories;

import javax.persistence.EntityManager;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.esupportail.emargement.security.ContextHelper;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EmargementRepositoryAspect {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private EntityManager em;

	// Attention, ne fonctionne pas sur les native query ...
	// De même cf doc hibernate "Filters apply to entity queries, but not to direct fetching."
	@Before(
			value="(execution(public * org.esupportail.emargement.repositories.*.*(..)) || execution(public * org.esupportail.emargement.repositories.custom.*.*(..)))" +
					"&& !execution(public * org.esupportail.emargement.repositories.*.findByContextKey(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.*.findByContextId(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.*.findByContext(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.*.findByContextAndId(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.*.findByContextAndEppn(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.AppliConfigRepository.findByContextAndKey(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.TagCheckerRepository.findByContextAndUserAppEppn(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.TagCheckRepository.countByContextAndSessionLocationExpectedId(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.TagCheckRepository.findByContextAndPersonEppnAndSessionEpreuve(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.TagCheckRepository.findByContextAndSessionLocationExpectedIdAndPersonEppnEquals(..)) " +
					"&& !execution(public * org.esupportail.emargement.repositories.TagCheckRepository.countByContextAndSessionLocationExpectedIdAndTagDateIsNotNull(..))",
			argNames="joinPoint")
	public void enableFilterIfNeeded(JoinPoint joinPoint) throws Throwable {
		String currentContext = ContextHelper.getCurrentContext();
		if(currentContext!=null && !currentContext.isEmpty() && !currentContext.equals("all")) {
			org.hibernate.Filter filter = em.unwrap(Session.class).enableFilter("contextFilter");
			Long id = ContextHelper.getCurrenyIdContext();
			if(id!=null) {
				filter.setParameter("context", id);
				filter.validate();
			} else {
				log.warn("Impossible de trouver le contexte de clé " + currentContext + " / joinPoint : " + joinPoint);
			}
		}
	}
	
	@After(
			value="(execution(public * org.esupportail.emargement.repositories.*.*(..)) || execution(public * org.esupportail.emargement.repositories.custom.*.*(..)))",
			argNames="joinPoint")
	public void disableFilter(JoinPoint joinPoint) throws Throwable {
		em.unwrap(Session.class).disableFilter("contextFilter");
	}
}
