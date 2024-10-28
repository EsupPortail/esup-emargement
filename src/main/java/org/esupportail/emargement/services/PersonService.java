package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {
	
	@Autowired
	private LdapUserRepository userLdapRepository;
	
	@Autowired
	private PersonRepository personRepository;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public List<Person> setNomPrenom(List<Person> persons){
		
		if(!persons.isEmpty()) {
			for(Person p : persons) {
				List<LdapUser> userLdaps = userLdapRepository.findByEppnEquals(p.getEppn());
				if(!userLdaps.isEmpty()) {
					p.setNom(userLdaps.get(0).getName());
					p.setPrenom(userLdaps.get(0).getPrenom());
				}
			}
		}
		return persons;
	}
	
	public List<String> getTypesPerson(){
		List<String> types = new ArrayList<>();
		types.add("student");
		types.add("staff");
		return types;
	}
	
	@Transactional
	public void deleteUnusedPersons(Context ctx) {
		int clean = personRepository.cleanPersons(ctx.getId());
		if(clean>0) {
			logService.log(ACTION.CLEAN_PERSONS, RETCODE.SUCCESS, " Nombre " + clean, null, null, ctx.getKey(), null);
		}else {
			log.info("Aucune personne à nettoyer");
		}
	}

}
