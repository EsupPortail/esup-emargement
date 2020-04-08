package org.esupportail.emargement.services;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.ContextRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.repositories.SessionLocationRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.esupportail.emargement.repositories.TagCheckerRepository;
import org.esupportail.emargement.repositories.UserAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Autowired	
	SessionLocationRepository sessionLocationRepository;
	
	@Autowired	
	TagCheckerRepository tagCheckerRepository;
	
	@Autowired	
	LocationRepository locationRepository;
	
	@Autowired	
	UserAppRepository userAppRepository;
	
	@Autowired	
	TagCheckRepository tagCheckRepository;
	
	@Autowired	
	CampusRepository campusRepository;
	
	@Autowired
	ContextRepository contextRepository;
	
    public LinkedHashMap<String, Object> mapField(List<Object> listes, int level){
    	
    	LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
    	
    	LinkedHashMap<String, Object> secondMap = new LinkedHashMap<String, Object>();
    	
    	String test = null;
    	int i = 1;
    	
   		for (Object result : listes) {
   			Object[] r = (Object[]) result;	
   			if (level == 2){
   				String r0 = "Aucune donnée";
   				if(r[0]!=null){
   					r0 = r[0].toString();
   				}
	   		    map.put(r0,r[1]);
   			}else{
   				//Hack '_' pour ne pas changer pas l'ordre de la requête dans le navigateur
   				if(test== null || test.equals("_" + r[0].toString())){
   					secondMap.put(r[1].toString(),r[2]);
   				}else{
   					map.put(test, secondMap);
   					secondMap = new LinkedHashMap<String, Object>();
   					secondMap.put(r[1].toString(),r[2]);
   				}
   			//Hack '_' pour ne pas changer pas l'ordre de la requête dans le navigateur
   				test = "_" +  r[0].toString();
   				if(i ==  listes.size()){
   					map.put(test, secondMap);
   				}
   				i++;
   			}
   		}
        return map;
    }
    
    @SuppressWarnings("serial")
	public  LinkedHashMap<String,Object> getStats(String typeStats, String key, String param) throws ParseException {
			
    	Context ctx = contextRepository.findByContextKey(key);
		LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>() {
			   
	        {
	        	if("sessionEpreuvesByCampus".equals(typeStats)){
	        		put("sessionEpreuvesByCampus",mapField(sessionEpreuveRepository.countSessionEpreuveByCampus(ctx.getId()), 2));
	        	}else if("sessionLocationByLocation".equals(typeStats)){
	        		put("sessionLocationByLocation",mapField(sessionLocationRepository.countSessionLocationByLocation(ctx.getId()), 2));
	        	}else if("tagCheckersByContext".equals(typeStats)){
	        		put("tagCheckersByContext",mapField(tagCheckerRepository.countTagCheckersByContext(ctx.getId()), 2));
	        	}else if("presenceByContext".equals(typeStats)){
	        		put("presenceByContext",mapField(tagCheckRepository.countPresenceByContext(ctx.getId()), 2));
	        	}else if("sessionEpreuveByYearMonth".equals(typeStats)){
	        		put("sessionEpreuveByYearMonth",mapField(sessionEpreuveRepository.countSessionEpreuveByYearMonth(ctx.getId()), 3));
	        	}else if("countTagCheckByYearMonth".equals(typeStats)){
	        		put("countTagCheckByYearMonth",mapField(tagCheckRepository.countTagCheckByYearMonth(ctx.getId()), 3));
	        	}else if("countCampusesByContext".equals(typeStats)){
	        		put("countCampusesByContext",mapField(tagCheckRepository.countTagCheckByYearMonth(ctx.getId()), 3));
	        	}else if("countTagChecksByTimeBadgeage".equals(typeStats)){
	        		put("countTagChecksByTimeBadgeage",mapField(tagCheckRepository.countTagChecksByTimeBadgeage(Long.valueOf(param)), 2));
	        	}else if("countTagChecksByTypeBadgeage".equals(typeStats)){
	        		put("countTagChecksByTypeBadgeage",mapField(tagCheckRepository.countTagChecksByTypeBadgeage(ctx.getId()), 2));
	        	}else if("countTagCheckBySessionLocationBadgedAndPerson".equals(typeStats)){
	        		put("countTagCheckBySessionLocationBadgedAndPerson",mapField(tagCheckRepository.countTagCheckBySessionLocationBadgedAndPerson(ctx.getId()), 2));
	        	}
	        }
	    };
		return results;
    }
    
    @SuppressWarnings("serial")
	public  LinkedHashMap<String,Object> getStatsSuperAdmin(String typeStats) throws ParseException {
			
		LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>() {
			   
	        {
	        	if("sessionEpreuvesByContext".equals(typeStats)){
	        		put("sessionEpreuvesByContext",mapField(sessionEpreuveRepository.countAllSessionEpreuvesByContext(), 3));
	        	}else if("countTagChecksByContext".equals(typeStats)){
	        		put("countTagChecksByContext",mapField(tagCheckRepository.countTagChecksByContext(), 3));
	        	}else if("countNbTagCheckerByContext".equals(typeStats)){
	        		put("countNbTagCheckerByContext",mapField(tagCheckerRepository.countNbTagCheckerByContext(), 2));
	        	}else if("countLocationsByContext".equals(typeStats)){
	        		put("countLocationsByContext",mapField(locationRepository.countLocationsByContext(), 2));
	        	}else if("countUserAppsByContext".equals(typeStats)){
	        		put("countUserAppsByContext",mapField(userAppRepository.countUserAppsByContext(), 3));
	        	}else if("countCampusesByContext".equals(typeStats)){
	        		put("countCampusesByContext",mapField(campusRepository.countCampusesByContext(), 2));
	        	}
	        }
	    };
		return results;
    }
}
