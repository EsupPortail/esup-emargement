package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;

import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.UserLdap;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.UserLdapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {
	
	@Autowired
	private UserLdapRepository userLdapRepository;
	
	@Autowired
	private PersonRepository personRepository;
	
	public List<Person> setNomPrenom(List<Person> persons){
		
		if(!persons.isEmpty()) {
			for(Person p : persons) {
				List<UserLdap> userLdaps = userLdapRepository.findByEppnEquals(p.getEppn());
				if(!userLdaps.isEmpty()) {
					p.setNom(userLdaps.get(0).getUsername());
					p.setPrenom(userLdaps.get(0).getPrenom());
				}
			}
		}
		return persons;
	}
	
	public List<String> getTypesPerson(){
		List<String> types = new ArrayList<String>();
		types.add("student");
		types.add("staff");
		return types;
	}

}
