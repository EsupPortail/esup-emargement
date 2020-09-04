package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Archive;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ArchiveService {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired	
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
	@Value("${app.nomDomaine}")
	private String nomDomaine;
	
	private static final String ANONYMOUS = "anonymous";
	

	public List<Archive>  getArchivesList(String emargementContext) {
		Context ctx = contextRepository.findByKey(emargementContext);
		List<String> anneeUnivs = sessionEpreuveRepository.findDistinctAnneeUniv(ctx.getId());
		List<Archive> archives = new ArrayList<Archive>();
		if(!anneeUnivs.isEmpty()) {
			for(String annee : anneeUnivs) {
				Archive archive = new Archive();
				archive.setAnneeUniv(annee);
				Long countSE = sessionEpreuveRepository.countByAnneeUniv(annee);
				archive.setNbSessions(countSE);
				Long countTc = tagCheckRepository.countTagCheckByAnneeUnivAndContextId(annee, ctx.getId());
				archive.setNbTagChecks(countTc);;
				Date firstDate = sessionEpreuveRepository.findFirstDateExamen(annee, ctx.getId());
				archive.setFirstDate(firstDate);
				Date lastDate = sessionEpreuveRepository.findLastDateExamen(annee, ctx.getId());
				archive.setLastDate(lastDate);
				List<SessionEpreuve> ses = sessionEpreuveRepository.findAByAnneeUnivAndDateArchivageIsNotNull(annee);
				if(!ses.isEmpty()) {
					archive.setDateArchivage(ses.get(0).getDateArchivage());
					archive.setLoginArchivage(ses.get(0).getLoginArchivage());
				}
				long nbAnonymousTagChecks = new Long (0);
				String anonymousEppn = ANONYMOUS.concat("_").concat(emargementContext).concat("@").concat(nomDomaine);
				List<Person> persons = personRepository.findByEppn(anonymousEppn);
				if(!persons.isEmpty()) {
					nbAnonymousTagChecks = tagCheckRepository.countAnonymousTagCheckBySAnneeUnivAndContextId(annee, ctx.getId(), persons.get(0).getId());
				}
				archive.setNbAnonymousTagChecks(nbAnonymousTagChecks);
				archives.add(archive);
			}
		}
		
		return archives;
	}
}
