package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.AppUser;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.GuestRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupeService {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired	
	GuestRepository guestRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Resource
	PersonService personService;
	
	public void computeCounters(List<Groupe> groupes) {
		for(Groupe groupe : groupes) {
			int  count = groupe.getPersons().size();
			int count2 = groupe.getGuests().size();
			if((count + count2) >0) {
				groupe.setNbTagCheck(count + count2);
			}
		}
	}
	
	public List<Groupe> getNotEmptyGroupes(){
		
		List<Groupe> groupes =  groupeRepository.findAll(Sort.by(Sort.Direction.ASC, "nom"));
		List<Groupe> notEmptyGroupes = new ArrayList<Groupe>();
		if(!groupes.isEmpty()) {
			for(Groupe groupe : groupes) {
				int  count = groupe.getPersons().size();
				int count2 = groupe.getGuests().size();
				if((count + count2) >0) {
					groupe.setNbTagCheck(count + count2);
					notEmptyGroupes.add(groupe);
				}
			}
		}
		
		return notEmptyGroupes;
	}
	
	public void addMember(String identifiant, List<Long> groupeIds) {
    	for(Long id : groupeIds) {
    		Groupe groupe = groupeRepository.findById(id).get();
    		List<Person> persons = personRepository.findByEppn(identifiant);
    		if(!persons.isEmpty()) {
    			groupe.getPersons().add(persons.get(0));
    		}else {
    			List<Guest> guests = guestRepository.findByEmail(identifiant);
        		if(!guests.isEmpty()){
        			groupe.getGuests().add(guests.get(0));
        		}
    		}
    		groupeRepository.save(groupe);
    	}
	}
	
	public void addPerson(Person person, List<Long> groupeIds) {
    	for(Long id : groupeIds) {
    		Groupe groupe = groupeRepository.findById(id).get();
    		groupe.getPersons().add(person);
    		groupeRepository.save(groupe);
    	}
	}
	
	public void addMembersFromSessionEpreuve(List<Long> ids, List<Long> groupeIds) {
		
		List<Person> allPersons =new ArrayList<Person>();
		List<Guest> allGuests =new ArrayList<Guest>();
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		
		for(Long id : ids) {
			SessionEpreuve se = sessionEpreuveRepository.findById(id).get();
			List<TagCheck> tcs = tagCheckRepository.findTagCheckBySessionEpreuveId(se.getId());
			List<Person> persons = tcs.parallelStream()
                    .map(TagCheck::getPerson).distinct().collect(Collectors.toList());
			
			allPersons.addAll(persons);
			
			List<Guest> guests = tcs.parallelStream()
                    .map(TagCheck::getGuest).distinct().collect(Collectors.toList());
			allGuests.addAll(guests);
			
		}
		for(Long id : groupeIds) {
    		Groupe groupe = groupeRepository.findById(id).get();
    		groupe.getPersons().addAll(allPersons);
    		groupe.getGuests().addAll(allGuests);
    		groupe.setDateModification(new Date());
    		groupe.setModificateur(eppn);
    		groupeRepository.save(groupe);
    	}
	}
	
	public void addMembersFromGroupe(List<Long> gr1Ids, List<Long> gr2Ids) {
		
		List<Person> allPersons =new ArrayList<Person>();
		List<Guest> allGuests =new ArrayList<Guest>();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String eppn = auth.getName();
		
		for(Long id : gr1Ids) {
			Groupe groupe = groupeRepository.findById(id).get();
			allPersons.addAll(groupe.getPersons());
			allGuests.addAll(groupe.getGuests());
		}
		for(Long id : gr2Ids) {
    		Groupe groupe = groupeRepository.findById(id).get();
    		groupe.getPersons().addAll(allPersons);
    		groupe.getGuests().addAll(allGuests);
    		groupe.setDateModification(new Date());
    		groupe.setModificateur(eppn);
    		groupeRepository.save(groupe);
    	}
	}
	
	public List <AppUser> getMembers(Long id){
    	Groupe groupe = groupeRepository.findById(id).get();
    	List<Person> personsList = new ArrayList<Person>(groupe.getPersons());
    	personService.setNomPrenom(personsList);
    	List<AppUser> appUsers = new ArrayList<AppUser>();
    	for(Person p : personsList) {
    		AppUser appUser = new AppUser();
    		appUser.setPersonOrGuestId(p.getId());
    		appUser.setEppnOrEmail(p.getEppn());
    		appUser.setNbSessions(0);
    		appUser.setNbGroupes(0);
    		appUser.setNom(p.getNom());
    		appUser.setNumEtu(p.getNumIdentifiant());
    		appUser.setPrenom(p.getPrenom());
    		appUser.setType(p.getType());
    		appUsers.add(appUser);
    	}
    	List<Guest> guestsList = new ArrayList<Guest>(groupe.getGuests());
    	for(Guest g: guestsList) {
    		AppUser appUser = new AppUser();
    		appUser.setPersonOrGuestId(g.getId());
    		appUser.setEppnOrEmail(g.getEmail());
    		appUser.setNbSessions(0);
    		appUser.setNbGroupes(0);
    		appUser.setNom(g.getNom());
    		appUser.setPrenom(g.getPrenom());
    		appUser.setType("ext");
    		appUsers.add(appUser);
    	}
    	appUsers.sort(Comparator.comparing(AppUser::getNom, Comparator.nullsFirst(Comparator.naturalOrder())));

    	return appUsers;
	}
	
	@Transactional
	public void delete(Groupe groupe) {
		groupe.getPersons().removeAll(groupe.getPersons());
		groupe.getGuests().removeAll(groupe.getGuests());
		List<SessionEpreuve> blackList = sessionEpreuveRepository.findByBlackListGroupe(groupe);
		if(!blackList.isEmpty()) {
			for(SessionEpreuve se : blackList) {
				se.setBlackListGroupe(null);
				sessionEpreuveRepository.save(se);
			}
		}
		groupeRepository.delete(groupe);
	}
	
	@Transactional
	public void deleteMembers(List<String> keys, Groupe groupe) {
		for(String key : keys) {
			String [] splitKey = key.split("@@");
			Long id = Long.valueOf(splitKey[0]);
			String type = splitKey[1];
			if("ext".equals(type)) {
				Guest guest = guestRepository.findById(id).get();
				if(guest != null) {
					groupe.getGuests().remove(guest);
				}
			}else{
				Person person = personRepository.findById(id).get();
				if(person != null) {
					groupe.getPersons().remove(person);
				}
			}
		}
		groupe.setDateModification(new Date());
		groupeRepository.save(groupe);
	}
	
	public List<String> getUsersForImport(List<Long> ids) {
		List<String> listUsers = new ArrayList<String>();
		if(!ids.isEmpty()) {
			for(Long id :ids) {
				Groupe groupe = groupeRepository.findById(id).get();
				groupe.getPersons();
				List<String> listEppn = groupe.getPersons().parallelStream()
		                .map(Person::getEppn).distinct().collect(Collectors.toList());
				if(!listEppn.isEmpty()) {
					listUsers.addAll(listEppn);
				}
				List<String> listEmail = groupe.getGuests().parallelStream()
		                .map(Guest::getEmail).distinct().collect(Collectors.toList());
				if(!listEmail.isEmpty()) {
					listUsers.addAll(listEmail);
				}
			}
		}
		return listUsers;
	}
	
	public String getNomFromGroupes(List<Long> ids) {
		
		String noms = "";
		List<String> nomsList = new ArrayList<String>();
		for(Long id : ids) {
			Groupe groupe = groupeRepository.findById(id).get();
			nomsList.add(groupe.nom);
		}
		if(! nomsList.isEmpty()) {
			noms = StringUtils.join(nomsList , ",");
		}
		
		return noms;
	}
	
	public boolean isBlackListed(Groupe gpe, String eppn) {
		boolean isBlackListed = false;
		if(gpe != null) {
			List<Person> persons = personRepository.findByEppn(eppn);
			if(!persons.isEmpty()) {
				Set<Person> gpePers = gpe.getPersons();
				if(!gpePers.isEmpty()) {
					if(gpePers.contains(persons.get(0))) {
						isBlackListed = true;
					}
				}
			}
		}
		return isBlackListed;
	}
}
