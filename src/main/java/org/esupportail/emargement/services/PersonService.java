package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;

import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.repositories.LdapUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {
	
	@Autowired
	private LdapUserRepository ldapUserRepository;
	
	public List<Person> setNomPrenom(List<Person> persons){
		
		if(!persons.isEmpty()) {
			for(Person p : persons) {
				List<LdapUser> ldapUsers = ldapUserRepository.findByEppnEquals(p.getEppn());
				if(!ldapUsers.isEmpty()) {
					p.setNom(ldapUsers.get(0).getName());
					p.setPrenom(ldapUsers.get(0).getPrenom());
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
