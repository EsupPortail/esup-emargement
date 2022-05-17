package org.esupportail.emargement.services;

import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.Prefs;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.PrefsRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
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
	
	public void updatePrefs(String nom, String value, String eppn, String key) {
		List<Prefs> prefs = prefsRepository.findByUserAppEppnAndNom(eppn, nom);
		Prefs pref = null;
		if(!prefs.isEmpty()) {
			pref = prefs.get(0);
			pref.setValue(value);
		}else {
			Context context = contextRepository.findByContextKey(key);
			pref = new Prefs();
			UserApp userApp = userAppRepository.findByEppnAndContext(eppn, context);
			pref.setUserApp(userApp);
			pref.setContext(context);
			pref.setNom(nom);
			pref.setValue(value);
		}
		prefsRepository.save(pref);
	}
}
