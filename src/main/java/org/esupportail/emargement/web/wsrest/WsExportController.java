package org.esupportail.emargement.web.wsrest;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagCheckBean;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.TagCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wsrest/export")
public class WsExportController {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;

	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	TagCheckService tagCheckService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	@GetMapping(value = "/tagCheck/{sessionId}", produces = "application/json;charset=UTF-8")
	public List<TagCheckBean> getInscritsFromSession(@PathVariable String sessionId) {
		List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(Long.valueOf(sessionId));
		List<TagCheckBean> beans =  tagCheckService.getBeansFromTagChecks(tcs);
		/*List<EntityModel<TagCheckBean>> entitiesBean = beans.stream()
			      .map(bean -> EntityModel.of(bean)).collect(Collectors.toList());*/
		return  beans;
	}
	
	@GetMapping(value = "/session/{contextId}", produces = "application/json;charset=UTF-8")
	public List<SessionEpreuve> getSessionsFromContext(@PathVariable Long contextId,
			@RequestParam(value="anneeUniv", required=false) String anneeUniv, 
			@RequestParam(value="sort", required=false)	String sort,
			@RequestParam(value="limit", required=false) String limit){
		Context ctx  = contextRepository.findById(contextId).get();
		List<SessionEpreuve> ses = sessionEpreuveRepository.findByContextOrderByDateExamenDescHeureEpreuveAscFinEpreuveAsc(ctx);
		sessionEpreuveService.computeCounters(ses);
		return  ses;
	}
}
