package org.esupportail.emargement.config;

import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.services.AbsenceService;
import org.esupportail.emargement.services.AppliConfigService;
import org.esupportail.emargement.services.TypeSessionService;
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

    @Override
    public void run(String... args) throws Exception {
        List<Context> contexts = contextRepository.findAll();
        if(!contexts.isEmpty()) {
        	for (Context context : contexts) {
        		appliConfigService.updateDescription(context); 
        		appliConfigService.updateAppliconfig(context);
        		appliConfigService.updateCatIsMissing(context);
        		typeSessionService.updateTypeSession(context.getKey());
        		absenceService.updateMotifAbsence(context.getKey());
        		if(context.getIsActif()==null) {
        			context.setIsActif(true);
        			contextRepository.save(context);
        		}
        	}
        }
    }
}