package org.esupportail.emargement.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.TypeSession;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class TypeSessionService {
	
	@Autowired
	TypeSessionRepository typeSessionRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	LogService logService;
	
	@Autowired
    private MessageSource messageSource;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public List<String> getTypeSessionCategories(){
		List<String> list = Arrays.asList("TD", "TP", "CONF", "CM", "REU", "EXA", "SEM", "COL", "CONC", "FOR", "EXPO");
		Collections.sort(list);
		return list;
	}
	
	@Transactional
	public int updateTypeSession(String emargementcontext) {
		List <String> list = getTypeSessionCategories();
		int nb= 0;
		if(!list.isEmpty()) {
			for(String key : list) {
				TypeSession typeSession = new TypeSession();
				typeSession.setDateModification(new Date());
				Context ctx = contextRepository.findByKey(emargementcontext);
				typeSession.setContext(ctx);
				typeSession.setLibelle(messageSource.getMessage("typeSession.libelle.".concat(key.toLowerCase()), null, null));
				typeSession.setKey(key.toUpperCase());
				typeSession.setAddByAdmin(true);
				typeSessionRepository.save(typeSession);
				nb++;
			}
			log.info("Ajout de rubriques d'aide : " + StringUtils.join(list, ", "));
			logService.log(ACTION.AJOUT_TYPESESSION, RETCODE.SUCCESS, "Ajout de types de session : " + StringUtils.join(list, ", "), null,  null, "all", null);
		}
		return nb;
	}
}
