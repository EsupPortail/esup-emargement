package org.esupportail.emargement.repositories;

import javax.persistence.EntityManager;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.services.ContextService;
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
	
	@Autowired
	ContextService contextService;
	
	@Autowired
	UserAppRepositoryCustom userAppRepositoryCustom;

	// Attention, ne fonctionne pas sur les native query ...
	// De même cf doc hibernate "Filters apply to entity queries, but not to direct fetching."
	@Before("this(org.springframework.data.repository.Repository) || within(org.esupportail.emargement.repositories.custom..*)")
	public void aroundExecution() throws Throwable {
		String currentContext = ContextHelper.getCurrentContext();
		if(currentContext!=null && !"login".equals(currentContext) && !"all".equals(currentContext) && !"wsrest".equals(currentContext)) {
			org.hibernate.Filter filter = em.unwrap(Session.class).enableFilter("contextFilter");
			Long id = ContextHelper.getCurrenyIdContext();
			if(id!=null) {
				filter.setParameter("context", id);
				filter.validate();
			}else {
				log.error("Imossible de trouver le contexte de clé " + currentContext);
			}

		}
	}

}
