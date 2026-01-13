package org.esupportail.emargement.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
	
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	public List mapFieldWith2Labels(List<Object[]> queryResults, boolean order) {
    	
    	List data = new ArrayList<>();
    	
    	List<String> labels1 = new ArrayList<String>();
    	for(Object[] r : queryResults) {
    		if(!labels1.contains(r[0].toString())) {
    			labels1.add(r[0].toString());
    		}
    	}   	
    	data.add(labels1);
    	
        List<String> labels2 = new ArrayList<String>();
        for(Object[] r : queryResults) {
        	if(!labels2.contains(r[1].toString())) {
        		labels2.add(r[1].toString());
        	}
    	}    	
    	
        Map<String, List<Long>> valuesMap = new HashMap<String, List<Long>>();
    	for(String label2: labels2) {
    		ArrayList<Long> values = new ArrayList<Long>();
    		// initialize to 0
    		for(String label1: labels1) {
    			values.add(0L);
    		}
    		for(Object[] r : queryResults) {
    	       	if(label2.equals(r[1].toString())) {
    	       		values.set(labels1.indexOf(r[0].toString()), Long.valueOf(r[2].toString()));
    	       	}
    		 }
    		valuesMap.put(label2, values);
    	}
    	if(order) {
	    	// order valuesMap
	    	Map<String, List<Long>> valuesMapSorted = valuesMap
	    	        .entrySet()
	    	        .stream()
	    	        .sorted(Entry.comparingByValue(new Comparator<List<Long>>() {
						@Override
						public int compare(List<Long> o1, List<Long> o2) {
							Long v1 = 0L;
							Long v2 = 0L;
							for(Long s: o1) {
								v1 += s;
							}
							for(Long s: o2) {
								v2 += s;
							}
							return v2.compareTo(v1);
						}
					}))
	    	        .collect(
	    	            Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,
	    	                LinkedHashMap::new));
	    	
	    	data.add(valuesMapSorted);
    	} else {
    		data.add(valuesMap);
    	}
    	
        return data;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public List mapFieldWith1Labels(List<Object[]> queryResults) {
    	
    	List data = new ArrayList<>();
    	
    	List<String> labels1 = new ArrayList<String>();
    	for(Object[] r : queryResults) {
    		if(r[0] == null) {
    			labels1.add("");
    		} else if(!labels1.contains(r[0].toString())) {
    			labels1.add(r[0].toString());
    		}
    	}   	
    	data.add(labels1);

    	ArrayList<Long> values = new ArrayList<Long>();

    	for(Object[] r : queryResults) {
    	    values.add(Long.valueOf(r[1].toString()));
    	}
    	data.add(values);
    	
        return data;
    }
    
	public  LinkedHashMap<String,Object> getStats(String typeStats, String key, String param, String year) throws ParseException {
			
    	Context ctx = contextRepository.findByContextKey(key);
		LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>() {
			   
	        {
	    		String anneeUniv = (!"all".equals(year))?  year : "20%";

	        	if("sessionEpreuvesByCampus".equals(typeStats)){
	        		put("sessionEpreuvesByCampus",mapFieldWith1Labels(sessionEpreuveRepository.countSessionEpreuveByCampus(ctx.getId(), anneeUniv)));
	        	}else if("sessionLocationByLocation".equals(typeStats)){
	        		put("sessionLocationByLocation",mapFieldWith1Labels(sessionLocationRepository.countSessionLocationByLocation(ctx.getId(), anneeUniv)));
	        	}else if("tagCheckersByContext".equals(typeStats)){
	        		put("tagCheckersByContext",mapFieldWith1Labels(tagCheckerRepository.countTagCheckersByContext(ctx.getId(), anneeUniv)));
	        	}else if("presenceByContext".equals(typeStats)){
	        		put("presenceByContext",mapFieldWith1Labels(tagCheckRepository.countPresenceByContext(ctx.getId(), anneeUniv)));
	        	}else if("sessionEpreuveByYearMonth".equals(typeStats)){
	        		put("sessionEpreuveByYearMonth",mapFieldWith1Labels(sessionEpreuveRepository.countSessionEpreuveByYearMonth(ctx.getId(), anneeUniv)));
	        	}else if("countTagCheckByYearMonth".equals(typeStats)){
	        		put("countTagCheckByYearMonth",mapFieldWith1Labels(tagCheckRepository.countTagCheckByYearMonth(ctx.getId(), anneeUniv)));
	        	}else if("countTagChecksByTimeBadgeage".equals(typeStats)){
	        		put("countTagChecksByTimeBadgeage",mapFieldWith1Labels(tagCheckRepository.countTagChecksByTimeBadgeage(Long.valueOf(param))));
	        	}else if("countTagChecksByTypeBadgeage".equals(typeStats)){
	        		put("countTagChecksByTypeBadgeage",mapFieldWith1Labels(tagCheckRepository.countTagChecksByTypeBadgeage(ctx.getId(), anneeUniv)));
	        	}else if("countTagCheckBySessionLocationBadgedAndPerson".equals(typeStats)){
	        		put("countTagCheckBySessionLocationBadgedAndPerson",mapFieldWith1Labels(tagCheckRepository.countTagCheckBySessionLocationBadgedAndPerson(ctx.getId(), anneeUniv)));
	        	}else if("countSessionEpreuveByType".equals(typeStats)){
	        		put("countSessionEpreuveByType",mapFieldWith1Labels(sessionEpreuveRepository.countSessionEpreuveByType(ctx.getId(), anneeUniv)));
	        	}
	        }
	    };
		return results;
    }
    
	public  LinkedHashMap<String,Object> getStatsSuperAdmin(String typeStats,  String year) throws ParseException {
			
		LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>() {
			   
	        {
	        	String anneeUniv = (!"all".equals(year))?  year : "20%";
	        	
	        	if("sessionEpreuvesByContext".equals(typeStats)){
	        		put("sessionEpreuvesByContext",mapFieldWith2Labels(sessionEpreuveRepository.countAllSessionEpreuvesByContext(anneeUniv), true));
	        	}else if("countTagChecksByContext".equals(typeStats)){
	        		put("countTagChecksByContext",mapFieldWith2Labels(tagCheckRepository.countTagChecksByContext(anneeUniv), true));
	        	}else if("countLocationsByContext".equals(typeStats)){
	        		put("countLocationsByContext",mapFieldWith1Labels(locationRepository.countLocationsByContext()));
	        	}else if("countUserAppsByContext".equals(typeStats)){
	        		put("countUserAppsByContext",mapFieldWith2Labels(userAppRepository.countUserAppsByContext(), true));
	        	}else if("countCampusesByContext".equals(typeStats)){
	        		put("countCampusesByContext",mapFieldWith1Labels(campusRepository.countCampusesByContext()));
	        	}else if("countSessionEpreuveByTypeByContext".equals(typeStats)){
	        		put("countSessionEpreuveByTypeByContext",mapFieldWith2Labels(sessionEpreuveRepository.countSessionEpreuveByTypeByContext(anneeUniv), true));
	        	}
	        }
	    };
		return results;
    }
}
