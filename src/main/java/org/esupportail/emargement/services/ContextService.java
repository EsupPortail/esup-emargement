package org.esupportail.emargement.services;

import java.util.List;

import org.esupportail.emargement.domain.AppliConfig;
import org.esupportail.emargement.domain.BigFile;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.EsupSignature;
import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.domain.Guest;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Log;
import org.esupportail.emargement.domain.Person;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.domain.TagCheck;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.TypeSession;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.AppliConfigRepository;
import org.esupportail.emargement.repositories.BigFileRepository;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.EsupSignatureRepository;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.GuestRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.LogsRepository;
import org.esupportail.emargement.repositories.PersonRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.TypeSessionRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.esupportail.emargement.security.ContextHelper;
import org.esupportail.emargement.security.ContextUserDetails;
import org.esupportail.emargement.web.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	ContextRepository contextRepository;
	
	@Autowired
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired
	AppliConfigRepository appliConfigRepository;
	
	@Autowired
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	LogsRepository logsRepository;
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired
	CampusRepository campusRepository;
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired
	PersonRepository personRepository;
	
	@Autowired
	BigFileRepository bigFileRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	GuestRepository guestRepository;
	
	@Autowired
	TypeSessionRepository typeSessionRepository;
	
	@Autowired	
	EsupSignatureRepository esupSignatureRepository;
	
	public String getDefaultContext() {
		String defaultContext = null;
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if(auth != null
				&& auth.getPrincipal() != null
				&& auth.getPrincipal() instanceof UserDetails) {
	    	ContextUserDetails userDetails = (ContextUserDetails)auth.getPrincipal();
	    	List<String> availablecontexts =  userDetails.getAvailableContexts();
	    	if(!availablecontexts.isEmpty()) {
	    		if("all".equals(availablecontexts.get(0)) && availablecontexts.size()>1){
	    			defaultContext = availablecontexts.get(1);
	    		}
	    	}
		}
		List<String> availableContexts = WebUtils.availableContexts();
		log.debug("availableContexts : " + availableContexts);
		if(!availableContexts.isEmpty()) {
			if(defaultContext == null || !availableContexts.contains(defaultContext)) {
				defaultContext = availableContexts.get(0);
			}
		}
		if(auth!=null && defaultContext!=null) {
			log.info(String.format("defaultContext for %s : %s", auth.getPrincipal(), defaultContext));
		}
		return defaultContext;
	}
	
	public Context getcurrentContext() {
        Long id = ContextHelper.getCurrenyIdContext();
        if(id==null) {
        	return null;
        }
		return contextRepository.findById(id).get();
	}
	
	@Transactional
	public void deleteContext(Context context) throws Exception {
		
		List<AppliConfig> appliconfigs = appliConfigRepository.findAppliConfigByContext(context);
		appliConfigRepository.deleteAll(appliconfigs);
		List<SessionLocation> sessionLocations = sessionLocationRepository.findSessionLocationByContext(context);
		sessionLocationRepository.deleteAll(sessionLocations);
		List<Log> logs = logsRepository.findLogByContext(context);
		logsRepository.deleteAll(logs);
		List<TagChecker> tagCheckers = tagCheckerRepository.findTagCheckerByContext(context);
		tagCheckerRepository.deleteAll(tagCheckers);
		List<TagCheck> tagChecks = tagCheckRepository.findTagCheckByContext(context);
		tagCheckRepository.deleteAll(tagChecks);
		List<Groupe> groupes = groupeRepository.findByContext(context);
		groupeRepository.deleteAll(groupes);
		List<SessionEpreuve> sessionEpreuves =  sessionEpreuveRepository.findSessionEpreuveByContext(context);
		sessionEpreuveRepository.deleteAll(sessionEpreuves);
		List<Campus> campuses = campusRepository.findByContext(context);
		campusRepository.deleteAll(campuses);
		List<Location> locations = locationRepository.findLocationByContext(context);
		locationRepository.deleteAll(locations);
		List<Prefs> prefs = prefsRepository.findByContext(context);
		prefsRepository.deleteAll(prefs);
		List<UserApp> userApps =  userAppRepository.findByContext(context);
		userAppRepository.deleteAll(userApps);
		List<Person> persons =  personRepository.findByContext(context);
		personRepository.deleteAll(persons);
		List<Guest> guests = guestRepository.findByContext(context);
		guestRepository.deleteAll(guests);
		List<BigFile> bigFiles =  bigFileRepository.findByContext(context);
		bigFileRepository.deleteAll(bigFiles);
		List<StoredFile> storedFiles = storedFileRepository.findByContext(context);
		storedFileRepository.deleteAll(storedFiles);
		List<TypeSession> typeSessions = typeSessionRepository.findByContext(context);
		typeSessionRepository.deleteAll(typeSessions);
		List<EsupSignature> signs = esupSignatureRepository.findByContext(context);
		esupSignatureRepository.deleteAll(signs);
		contextRepository.delete(context);
	
	}
}
