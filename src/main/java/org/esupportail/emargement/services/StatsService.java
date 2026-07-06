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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.DoughnutChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.charts.PieChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.DoughnutData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.data.PieData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.DoughnutDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.dataset.PieDataset;

@Service
public class StatsService {
	
	private static final int[] ACADEMIC_MONTHS =
	    {9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8};
	private static final String[] ACADEMIC_MONTH_LABELS =
	    {"Sept", "Oct", "Nov", "Déc", "Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Août"};
	
	private final ObjectMapper objectMapper = new ObjectMapper();

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
	
    private String withStackedOptions(String chartJson) {
    	try {
    		ObjectNode root = (ObjectNode) objectMapper.readTree(chartJson);

    		ObjectNode xAxis = objectMapper.createObjectNode().put("stacked", true);
    		ObjectNode yAxis = objectMapper.createObjectNode().put("stacked", true);
    		ObjectNode scales = objectMapper.createObjectNode();
    		scales.set("x", xAxis);
    		scales.set("y", yAxis);

    		ObjectNode options = objectMapper.createObjectNode();
    		options.set("scales", scales);
    		root.set("options", options);

    		return objectMapper.writeValueAsString(root);
    	} catch (Exception e) {
    		return chartJson; // en cas de souci de parsing, on renvoie le graphique non stacké plutôt que rien
    	}
    }
	
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
    
    @SuppressWarnings("unchecked")
    private String pieOrDoughnut(List<Object[]> queryResults, boolean doughnut) {
    	List raw = mapFieldWith1Labels(queryResults);
    	List<String> labels = (List<String>) raw.get(0);
    	List<Long> values = (List<Long>) raw.get(1);

    	if (doughnut) {
    		DoughnutDataset ds = new DoughnutDataset();
    		values.forEach(ds::addData);
    		return new DoughnutChart(new DoughnutData().addLabels(labels.toArray(new String[0])).addDataset(ds)).toJson();
    	}
		PieDataset ds = new PieDataset();
		values.forEach(ds::addData);
		return new PieChart(new PieData().addLabels(labels.toArray(new String[0])).addDataset(ds)).toJson();
    }

    @SuppressWarnings("unchecked")
    private String bar(List<Object[]> queryResults, String label) {
    	List raw = mapFieldWith1Labels(queryResults);
    	List<String> labels = (List<String>) raw.get(0);
    	List<Long> values = (List<Long>) raw.get(1);

    	BarDataset ds = new BarDataset().setLabel(label);
    	values.forEach(ds::addData);
    	return new BarChart(new BarData().addLabels(labels.toArray(new String[0])).addDataset(ds)).toJson();
    }

    @SuppressWarnings("unchecked")
    private String line(List<Object[]> queryResults) {
    	List raw = mapFieldWith1Labels(queryResults);
    	List<String> rawLabels = (List<String>) raw.get(0); // "9", "10", "1", ...
    	List<Long> rawValues = (List<Long>) raw.get(1);

    	Map<Integer, Long> valuesByMonth = new HashMap<Integer, Long>();
    	for (int i = 0; i < rawLabels.size(); i++) {
    		try {
    			valuesByMonth.put(Integer.valueOf(rawLabels.get(i).trim()), rawValues.get(i));
    		} catch (NumberFormatException e) {
    			// libellé non numérique, on ignore
    		}
    	}

    	LineDataset ds = new LineDataset();
    	for (int month : ACADEMIC_MONTHS) {
    		ds.addData(valuesByMonth.getOrDefault(month, 0L));
    	}

    	return new LineChart(new LineData().addLabels(ACADEMIC_MONTH_LABELS).addDataset(ds)).toJson();
    }

    @SuppressWarnings("unchecked")
    private String stackedBar(List<Object[]> queryResults) {
    	List raw = mapFieldWith2Labels(queryResults, true);
    	List<String> labels = (List<String>) raw.get(0);
    	Map<String, List<Long>> valuesMap = (Map<String, List<Long>>) raw.get(1);

    	BarData data = new BarData().addLabels(labels.toArray(new String[0]));
    	for (Map.Entry<String, List<Long>> entry : valuesMap.entrySet()) {
    		BarDataset ds = new BarDataset().setLabel(entry.getKey());
    		entry.getValue().forEach(ds::addData);
    		data.addDataset(ds);
    	}

    	return withStackedOptions(new BarChart(data).toJson());
    }

	// ---- point d'entrée principal, renvoie directement le JSON Chart.js ----

	public String getStats(String typeStats, String key, String param, String year) throws ParseException {
		Context ctx = contextRepository.findByContextKey(key);
		String anneeUniv = (!"all".equals(year)) ? year : "20%";

		switch (typeStats) {
			case "sessionEpreuvesByCampus":
				return pieOrDoughnut(sessionEpreuveRepository.countSessionEpreuveByCampus(ctx.getId(), anneeUniv), false);
			case "sessionLocationByLocation":
				return pieOrDoughnut(sessionLocationRepository.countSessionLocationByLocation(ctx.getId(), anneeUniv), true);
			case "tagCheckersByContext":
				return pieOrDoughnut(tagCheckerRepository.countTagCheckersByContext(ctx.getId(), anneeUniv), false);
			case "presenceByContext":
				return pieOrDoughnut(tagCheckRepository.countPresenceByContext(ctx.getId(), anneeUniv), false);
			case "sessionEpreuveByYearMonth":
				return line(sessionEpreuveRepository.countSessionEpreuveByYearMonth(ctx.getId(), anneeUniv));
			case "countTagCheckByYearMonth":
				return line(tagCheckRepository.countTagCheckByYearMonth(ctx.getId(), anneeUniv));
			case "countTagChecksByTypeBadgeage":
				return pieOrDoughnut(tagCheckRepository.countTagChecksByTypeBadgeage(ctx.getId(), anneeUniv), true);
			case "countTagCheckBySessionLocationBadgedAndPerson":
				return pieOrDoughnut(tagCheckRepository.countTagCheckBySessionLocationBadgedAndPerson(ctx.getId(), anneeUniv), true);
			case "countSessionEpreuveByType":
				return pieOrDoughnut(sessionEpreuveRepository.countSessionEpreuveByType(ctx.getId(), anneeUniv), false);
			case "countTagChecksByTimeBadgeage":
				return bar(tagCheckRepository.countTagChecksByTimeBadgeage(Long.valueOf(param)), "Badgeages");
			default:
				return null;
		}
	}

	public String getStatsSuperAdmin(String typeStats, String year) throws ParseException {
		String anneeUniv = (!"all".equals(year)) ? year : "20%";

		switch (typeStats) {
			case "sessionEpreuvesByContext":
				return stackedBar(sessionEpreuveRepository.countAllSessionEpreuvesByContext(anneeUniv));
			case "countTagChecksByContext":
				return stackedBar(tagCheckRepository.countTagChecksByContext(anneeUniv));
			case "countLocationsByContext":
				return bar(locationRepository.countLocationsByContext(), "Locations");
			case "countUserAppsByContext":
				return stackedBar(userAppRepository.countUserAppsByContext());
			case "countCampusesByContext":
				return bar(campusRepository.countCampusesByContext(), "Campus");
			case "countSessionEpreuveByTypeByContext":
				return stackedBar(sessionEpreuveRepository.countSessionEpreuveByTypeByContext(anneeUniv));
			default:
				return null;
		}
	}
}
