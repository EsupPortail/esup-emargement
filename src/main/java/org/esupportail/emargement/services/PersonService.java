package org.esupportail.emargement.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.LdapUser;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;


@Service
public class PersonService {
	
	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private UserAppRepository userAppRepository;
	
	@Resource
	LogService logService;
	
	@Resource
	LdapService ldapService;

	@Resource
	AppliConfigService appliConfigService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public List<Person> setNomPrenom(List<Person> persons){
		List<String> personList = persons.stream().filter(person -> person.getEppn()!=null).map(person -> person.getEppn())
				.collect(Collectors.toList());
		Map<String, LdapUser> mapLdapUsers = ldapService.getLdapUsersFromNumList(personList, "eduPersonPrincipalName");
		if(!persons.isEmpty()) {
			for(Person p : persons) {
				LdapUser ldapUser = mapLdapUsers.get(p.getEppn());
				if(ldapUser!=null) {
					p.setNom(ldapUser.getName());
					p.setPrenom(ldapUser.getPrenom());
					p.setCivilite(ldapUser.getCivilite());
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

	public List<String> getFormationTypes(Context ctx) {
		return personRepository.findDistinctFormationTypesByContext(ctx);
	}

	public List<String> getRegimeChoices(String emargementContext, Context context) {
		LinkedHashSet<String> regimes = new LinkedHashSet<>();
		if (emargementContext != null) {
			for (String regime : appliConfigService.getAdeRegimeChoice(emargementContext)) {
				addRegime(regimes, regime);
			}
		}
		if (context != null) {
			for (String regime : getFormationTypes(context)) {
				addRegime(regimes, regime);
			}
		}
		return new ArrayList<>(regimes);
	}

	private void addRegime(LinkedHashSet<String> regimes, String regime) {
		if (regime == null) {
			return;
		}
		for (String part : regime.split("[,;]")) {
			if (!part.isBlank()) {
				regimes.add(part.trim());
			}
		}
	}
	public Person updatePerson(Person person, Context context, UserApp currentUserApp) {
		person.setUserAppUpdatedAt(LocalDateTime.now());
		if (currentUserApp != null) {
			person.setUserModif(currentUserApp);
		}
        if (person.getFormationType() != null && person.getFormationType().trim().isEmpty()) {
            person.setFormationType(null);
        }
        if (person.getId() != null) {
            personRepository.findById(person.getId()).ifPresent(existing -> {
                person.setFormasupLinked(existing.getFormasupLinked());
				person.setSyncUpdatedAt(existing.getSyncUpdatedAt());
				person.setCreatedAt(existing.getCreatedAt());
                if (person.getLockSyncLdap()) {
                    person.setEppn(existing.getEppn());
                    person.setNumIdentifiant(existing.getNumIdentifiant());
                    person.setType(existing.getType());
                }
            });
        }
		person.setContext(context);
		return personRepository.save(person);
	}

	public void deletePerson(Person person) {
		personRepository.delete(person);
	}

	public void createPerson(Person person, Context context, UserApp currentUserApp) {
		if (person.getFormationType() != null && person.getFormationType().trim().isEmpty()) {
            person.setFormationType(null);
        }
        person.setFormasupLinked(false);
        person.setContext(context);
		person.setCreatedAt(LocalDateTime.now());
		if (currentUserApp != null) {
			person.setUserModif(currentUserApp);
		}
        personRepository.save(person);
	}

	public Page<Person> getPersonsPage(Context context, String searchFilter, String regimeValue, boolean onlyBoursier, boolean onlyRse, boolean onlyTiersTemps, boolean onlyFormaSup, Pageable pageable) {
		Page<Person> personPage = personRepository.findAll(
			personFilter(searchFilter, regimeValue, onlyBoursier, onlyRse, onlyTiersTemps, onlyFormaSup),
			pageable);
		return personPage;
	}

	private Specification<Person> personFilter(String search, String regime, boolean boursier, boolean rse,
		boolean tiersTemps, boolean formaSup) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(search)) {
				String like = "%" + search.toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("eppn")), like),
						cb.like(root.get("numIdentifiant"), "%" + search + "%")));
			}
			if (StringUtils.hasText(regime)) {
				predicates.add(cb.equal(root.get("formationType"), regime));
			}
			if (boursier) {
				predicates.add(cb.isTrue(root.get("isBoursier")));
			}
			if (rse) {
				predicates.add(cb.isTrue(root.get("isRSE")));
			}
			if (tiersTemps) {
				predicates.add(cb.isTrue(root.get("hasTierTempory")));
			}
			if (formaSup) {
				predicates.add(cb.isTrue(root.get("formasupLinked")));
			}
			if (predicates.isEmpty()) {
				return cb.conjunction();
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	public Person getPersonById(Long id) {
		return personRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Personne introuvable pour l'ID " + id));
	}
}