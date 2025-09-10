package org.esupportail.emargement.services;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PreferencesService {

	@Autowired
	PrefsRepository prefsRepository;
	
	@Autowired
	UserAppRepository userAppRepository;
	
	@Autowired	
	ContextRepository contextRepository;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public void updatePrefs(String nom, String value, String eppn, String key, String choice) {
		List<Prefs> prefs = null;
		if(nom.startsWith(choice, 0)) {
			prefs = prefsRepository.findByNomAllContexts(nom);
		}else {
			prefs = prefsRepository.findByUserAppEppnAndNom(eppn, nom);
		}
		Context context = contextRepository.findByContextKey(key);
		UserApp userApp = userAppRepository.findByEppnAndContext(eppn, context);
		Prefs pref = null;
		if(!prefs.isEmpty()) {
			pref = prefs.get(0);
		}else {
			pref = new Prefs();
			pref.setNom(nom);
		}
		pref.setContext(context);
		pref.setValue(value);
		pref.setUserApp(userApp);
		pref.setDateModification(new Date());
		prefsRepository.save(pref);
	}
	
	public void removePrefs(String eppn, String nom) {
		List<Prefs> prefs = null;
		if(eppn != null) {
			prefs = prefsRepository.findByUserAppEppnAndNomLike(eppn, nom + "%");
		}else {
			prefs = prefsRepository.findByNom(nom);
		}
		if(!prefs.isEmpty()) {
			prefsRepository.deleteAll(prefs);
			log.info("Suppression des préférences obsolètes : " + nom);
		}
	}
	
	public void cleanPrefs(Context ctx) {
		List<Prefs> prefs = null;
		String noms [] = {"adeStoredSession", "enableWebcam"};
		for(String nom : Arrays.asList(noms)) {
			prefs = prefsRepository.findByNomAndContext(nom, ctx);
			if(!prefs.isEmpty()) {
				prefsRepository.deleteAll(prefs);
				log.info("Suppression des préférences obsolètes : " + nom + " pour le contexte : " + ctx);
			}
		}
	}
}
