package org.esupportail.emargement.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;
import org.esupportail.emargement.domain.UserApp;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.custom.TagCheckerRepositoryCustom;
import org.esupportail.emargement.repositories.custom.UserAppRepositoryCustom;
import org.esupportail.emargement.services.LogService.ACTION;
import org.esupportail.emargement.services.LogService.RETCODE;
import org.esupportail.emargement.utils.ToolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionLocationService {
	
	@Autowired
	private SessionEpreuveRepository sessionEpreuveRepository;
	@Autowired
	private SessionLocationRepository sessionLocationRepository;
	@Autowired
	private LocationRepository locationRepository;
	@Autowired
	private TagCheckRepository tagCheckRepository;
	@Autowired
	private UserAppRepositoryCustom userAppRepositoryCustom;
	@Autowired
	private TagCheckerRepositoryCustom tagCheckerRepositoryCustom;
	
	@Autowired
	ToolUtil toolUtil;
	
	@Resource
	LogService logService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public HashMap <Long, List<Location>> getMapSessions(Long sessionEpreuveId, boolean locationsUsed){
		
		 HashMap <Long, List<Location>> mapSessions = new  HashMap <Long, List<Location>>();
		 
		 List<SessionEpreuve> allSessionEpreuves = sessionEpreuveRepository.findAll();
		 SessionEpreuve se = sessionEpreuveRepository.findById(sessionEpreuveId).get();
		 List<Location> locationsCampus = locationRepository.findLocationByCampus(se.getCampus());
		 List<Long> ids = new ArrayList<Long>();
		 for(Location l : locationsCampus ) {
			 ids.add(l.getId());
		 }
		 
		 List<SessionLocation> allSesssionLocations = sessionLocationRepository.findSessionLocationByLocationIdIn(ids);
		 
		 for(SessionEpreuve sessionEpreuve : allSessionEpreuves) {
			 List<Location> allLocations = locationRepository.findLocationByCampus(se.getCampus());
			 List<Location> newLocations = new ArrayList<Location>();
			 for(SessionLocation sessionLocation : allSesssionLocations) {
				if(sessionLocation.getSessionEpreuve().equals(sessionEpreuve)) {
					newLocations.add(sessionLocation.getLocation());
				}
			 }
			 if(locationsUsed) {
				 allLocations = newLocations;
			 }else {
				 allLocations.removeAll(newLocations);
			 }
			 if(sessionEpreuveId != 0 && sessionEpreuveId.equals(sessionEpreuve.getId())) {
				 mapSessions.put(sessionEpreuve.getId(), allLocations);
			 }else if(sessionEpreuveId == 0 || sessionEpreuveId==null) {
				 mapSessions.put(sessionEpreuve.getId(), allLocations);
			 }
		 }
		 
		 return mapSessions;
		 
	}
	
	public HashMap <Long, List<SessionLocation>> getMapSessionLocations(Long sessionEpreuveId, boolean locationsUsed){
		
		 HashMap <Long, List<SessionLocation>> mapSessionLocations = new  HashMap <Long, List<SessionLocation>>();
		 
		 List<SessionEpreuve> allSessionEpreuves = sessionEpreuveRepository.findAll();
		 
		 for(SessionEpreuve sessionEpreuve : allSessionEpreuves) {
			 List<SessionLocation> allSesssionLocations = sessionLocationRepository.findAll();
			 List<SessionLocation> newSessionLocations = new ArrayList<SessionLocation>();
			 for(SessionLocation sessionLocation : allSesssionLocations) {
				if(sessionLocation.getSessionEpreuve().equals(sessionEpreuve)) {
					newSessionLocations.add(sessionLocation);
				}
			 }
			 if(locationsUsed) {
				 allSesssionLocations = newSessionLocations;
			 }else {
				 allSesssionLocations.removeAll(newSessionLocations);
			 }
			 if(sessionEpreuveId != 0 && sessionEpreuveId.equals(sessionEpreuve.getId())) {
				 mapSessionLocations.put(sessionEpreuve.getId(), allSesssionLocations);
			 }else if(sessionEpreuveId == 0 || sessionEpreuveId==null) {
				 mapSessionLocations.put(sessionEpreuve.getId(), allSesssionLocations);
			 }
		 }
		 
		 return mapSessionLocations;
		 
	}
	
    public List<SessionLocation> getRepartition(Long sessionEpreuveid){
    	
    	List<SessionLocation> repartitions = sessionLocationRepository.findSessionLocationBySessionEpreuveIdOrderByIsTiersTempsOnlyAscPrioriteAscCapaciteDesc(sessionEpreuveid);
    	
    	if(!repartitions.isEmpty()) {
    		for(SessionLocation sl : repartitions) {
    			Long count =tagCheckRepository.countBySessionLocationExpected(sl);
    			sl.setNbInscritsSessionLocation(count);
    			if(sl.getCapacite()!=0 && count !=0) {
	    			int taux = count.intValue()* 100/sl.getCapacite();
    				Long tauxRemplissage = new Long(taux);
	    			sl.setTauxRemplissage(tauxRemplissage);
    			}
    		}
    	}
    	
    	return  repartitions;
    }

    public List<String> findWsRestLocations(String eppn, String emargementContext){
    	
    	List<String> locations = new ArrayList<String>();
    	try {
			List<UserApp> userApps = userAppRepositoryCustom.findByEppn(eppn);
			log.info("Nb userApp : " + userApps.size());
			List<TagChecker> tagCheckers = tagCheckerRepositoryCustom.findTagCheckerByUserAppIn(userApps);
			log.info("Nb TagChecker : " + tagCheckers.size());
			for(TagChecker tc : tagCheckers) {
				int check =  toolUtil.compareDate(tc.getSessionEpreuve().getDateExamen(), new Date(), "yyyy-MM-dd");
				log.debug("var check :" + check);
				int checkIfDateFinIsOk = -1;
				if(tc.getSessionEpreuve().getDateFin() != null) {
					checkIfDateFinIsOk = toolUtil.compareDate(tc.getSessionEpreuve().getDateFin(), new Date(), "yyyy-MM-dd");
				}else {
					checkIfDateFinIsOk = check;
				}
				log.debug("var checkIfDateFinIsOk :" + checkIfDateFinIsOk);
				if(!Statut.CLOSED.equals(tc.getSessionEpreuve().getStatut()) && (check==0 || check<=0 && checkIfDateFinIsOk>=0)) {
					SessionLocation sl = tc.getSessionLocation();
					SessionEpreuve se = sl.getSessionEpreuve();
					DateFormat df = new SimpleDateFormat("HH:mm");
					String  heure = df.format(se.getHeureEpreuve());
					// location --> heureDebut//nomSession//nomSalle//idSession
					locations.add(heure.concat(" // ").concat(se.getNomSessionEpreuve()).concat(" // ").
							concat(sl.getLocation().getNom().concat(" // ").concat(se.getId().toString())));
				}
			}
			log.info("Récupération des lieux de session (" + StringUtils.join(locations, "'") + ") des Ws Rest pour l'eppn : " + eppn );
		} catch (Exception e) {
			log.error("Erreur de récupération des lieux de session pour les Ws Rest pour l'eppn : " + eppn );
			logService.log(ACTION.WSREST_LOCATIONS, RETCODE.FAILED, "Eppn : " + eppn, null, null, emargementContext, eppn);
			e.printStackTrace();
		}
    	Collections.sort(locations);
    	return locations;
    }
    
    public void deleteAllTLocationsBySessionEpreuveId(Long id) {
    	List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuveId(id);
    	sessionLocationRepository.deleteAll(sls);
    }
    
    public List<Integer> getPriorityList(SessionEpreuve se) {
    	List<Integer> priorities = new ArrayList<>();
    	List<SessionLocation> sls = sessionLocationRepository.findSessionLocationBySessionEpreuve(se);
    	if(!sls.isEmpty()) {
    		priorities = sls.stream().map(e->e.getPriorite()).collect(Collectors.toList());
    	}
    	
    	return priorities;
	}
}
