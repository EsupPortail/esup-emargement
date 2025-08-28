package org.esupportail.emargement.config;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.services.AbsenceService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.PreferencesService;
import org.esupportail.emargement.services.SessionEpreuveService;
import org.esupportail.emargement.services.TypeSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {
	
	@Autowired
	ContextRepository contextRepository;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Resource
	TypeSessionService typeSessionService;
	
	@Resource
	AbsenceService absenceService;
	
	@Resource
	PreferencesService preferencesService;
	
	@Resource
	SessionEpreuveService sessionEpreuveService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void run(String... args) throws Exception {
        List<Context> contexts = contextRepository.findAll();
        if(!contexts.isEmpty()) {
        	log.info("Tâches au démarrage de l'application : ");
        	for (Context context : contexts) {
        		String key = context.getKey();
        		log.info("-----Contexte : " +  key + " ------");
        		appliConfigService.updateDescription(context); 
        		appliConfigService.updateAppliconfig(context);
        		appliConfigService.updateCatIsMissing(context);
        		typeSessionService.updateTypeSession(key);
        		absenceService.updateMotifAbsence(key);
        		sessionEpreuveService.updateStatutSession(key);
        		sessionEpreuveService.migrateAllStatutSession(context);
        		preferencesService.cleanPrefs(context);
        		if(context.getIsActif()==null) {
        			context.setIsActif(true);
        			contextRepository.save(context);
        		}
        	}
        }
    }
}