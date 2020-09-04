package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Help;
import org.esupportail.emargement.repositories.HelpRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class HelpService {
	
	@Autowired
	HelpRepository helpRepository;
	
	@Autowired
    private MessageSource messageSource;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public List<String> getHelpCategories(){
		List<String> list = Arrays.asList("context", "admins", "help", "userApp", "campus", "location", "logs", "appliConfig", "tagCheck", "tagChecker", "event",
										"sessionEpreuve", "sessionLocation", "extraction", "repartition", "convocation", "consignes", "presence", "stats", "individu", 
										"calendrier", "su", "dashboard", "groupe", "user", "apps", "archives");
		Collections.sort(list);
		return list;
	}
	
	public String getValueOfKey(String key) {
		
		String  helpValue = "";
		
		if(!helpRepository.findByKey(key).isEmpty()){
			helpValue = helpRepository.findByKey(key).get(0).getValue();
		}
		
		return helpValue;
	}
	
	public List <String> checkHelp() {
		List <String> listKey = getHelpCategories();
		List <Help> list = helpRepository.findAll();
		List <String> currentKeys = list.stream().map(o -> o.getKey()).collect(Collectors.toList());
		List <String> newListKey = new ArrayList<String>();
		
		for (String key : listKey){
			if(!currentKeys.contains(key)) {
				newListKey.add(key);
				log.info("rubrique d'aide manquante: " + key);
			}
		}
		return newListKey;
	}
	
	@Transactional
	public int updateHelp() {
		List <String> list =  checkHelp();
		int nb= 0;
		if(!list.isEmpty()) {
			for(String key : list) {
				Help help = new Help();
				help.setDateModification(new Date());
				help.setDescription(messageSource.getMessage("help.desc.".concat(key), null, null));
				help.setKey(messageSource.getMessage("help.key.".concat(key), null, null));
				help.setValue(messageSource.getMessage("help.value.".concat(key), null, null));
				helpRepository.save(help);
				nb++;
			}
			log.info("Ajout de rubriques d'aide : " + StringUtils.join(list, ", "));
			logService.log(ACTION.AJOUT_HELP, RETCODE.SUCCESS, "Ajout de rubriques d'aide : " + StringUtils.join(list, ", "), null,  null, "all", null);
		}
		return nb;
	}

}
