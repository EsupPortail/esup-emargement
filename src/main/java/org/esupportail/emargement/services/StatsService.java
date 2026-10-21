package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.DoughnutData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.data.PieData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.DoughnutDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.dataset.PieDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.LegendOptions;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;

@Service
public class StatsService {
	
	private static final int[] ACADEMIC_MONTHS =
	    {9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8};
	private static final String[] ACADEMIC_MONTH_LABELS =
	    {"Sept", "Oct", "Nov", "Déc", "Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Août"};
    private static final int[][] PALETTE_RGB = {
    	    {54, 162, 235}, {255, 99, 132}, {255, 159, 64},
    	    {255, 205, 86}, {75, 192, 192}, {153, 102, 255}};
    private static final int[] GRIS_AUTRES = {201, 203, 207};
	
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
    	
    	List<String> labels1 = new ArrayList<>();
    	for(Object[] r : queryResults) {
    		if(r[0] == null) {
    			labels1.add("");
    		} else if(!labels1.contains(r[0].toString())) {
    			labels1.add(r[0].toString());
    		}
    	}   	
    	data.add(labels1);

    	ArrayList<Long> values = new ArrayList<>();

    	for(Object[] r : queryResults) {
    	    values.add(Long.valueOf(r[1].toString()));
    	}
    	data.add(values);
    	
        return data;
    }
    
    @SuppressWarnings("unchecked")
    private String pieOrDoughnut(List<Object[]> queryResults, boolean doughnut) {
    	List<Object> raw = mapFieldWith1Labels(queryResults);
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
    	List<Object> raw = mapFieldWith1Labels(queryResults);
    	List<String> labels = (List<String>) raw.get(0);
    	List<Long> values = (List<Long>) raw.get(1);

    	BarDataset ds = new BarDataset().setLabel(label);
    	values.forEach(ds::addData);
    	return new BarChart(new BarData().addLabels(labels.toArray(new String[0])).addDataset(ds)).toJson();
    }

    @SuppressWarnings("unchecked")
    private String line(List<Object[]> queryResults) {
    	List<Object> raw = mapFieldWith1Labels(queryResults);
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

		LineOptions options = new LineOptions()
				.setPlugins(new Plugins().setLegend(new LegendOptions().setDisplay(false)));

		return new LineChart(new LineData().addLabels(ACADEMIC_MONTH_LABELS).addDataset(ds), options).toJson();
    }

	private String lineByContext(List<Object[]> rows, int maxLignes) {
	    // rows : [ctx, annee, mois, cnt]
	    Map<String, Map<Integer, Long>> parContext = new LinkedHashMap<>();
	    Map<String, Long> totaux = new HashMap<>();
	    for (Object[] r : rows) {
	        String ctx = (String) r[0];
	        int mois = ((Number) r[2]).intValue();
	        long cnt = ((Number) r[3]).longValue();
	        parContext.computeIfAbsent(ctx, k -> new HashMap<>()).put(mois, cnt);
	        totaux.merge(ctx, cnt, Long::sum);
	    }

	    // contextes triés par total annuel décroissant : top N + « Autres »
	    List<String> ordre = totaux.entrySet().stream()
	            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
	            .map(Map.Entry::getKey)
	            .collect(Collectors.toList());
	    List<String> top = ordre.subList(0, Math.min(maxLignes, ordre.size()));
	    List<String> reste = ordre.subList(top.size(), ordre.size());

	    LineData data = new LineData().addLabels(ACADEMIC_MONTH_LABELS);

	    int i = 0;
	    for (String ctx : top) {
	        data.addDataset(dataset(ctx, parContext.get(ctx), PALETTE_RGB[i++ % PALETTE_RGB.length]));
	    }
	    if (!reste.isEmpty()) {
	        Map<Integer, Long> autres = new HashMap<>();
	        for (String ctx : reste) {
	            parContext.get(ctx).forEach((mois, cnt) -> autres.merge(mois, cnt, Long::sum));
	        }
	        data.addDataset(dataset("Autres (" + reste.size() + ")", autres, GRIS_AUTRES));
	    	
	    }
	    LineOptions options = new LineOptions()
    	        .setPlugins(new Plugins()
    	            .setLegend(new LegendOptions().setDisplay(false)));

	    return new LineChart(data, options).toJson();   // légende visible (par défaut)
	}

	private LineDataset dataset(String label, Map<Integer, Long> valuesByMonth, int[] rgb) {
	    LineDataset ds = new LineDataset().setLabel(label);
	    for (int month : ACADEMIC_MONTHS) {
	        ds.addData(valuesByMonth.getOrDefault(month, 0L));
	    }
	    ds.setBorderColor(new RGBAColor(rgb[0], rgb[1], rgb[2]));
	    return ds;
	}

	@SuppressWarnings("unchecked")
	private String stackedBar(List<Object[]> queryResults) {
	    List<Object> raw = mapFieldWith2Labels(queryResults, true);
	    List<String> labels = (List<String>) raw.get(0);
	    Map<String, List<Long>> valuesMap = (Map<String, List<Long>>) raw.get(1);

	    int[] order = orderDesc(labels.size(), valuesMap, null);   // null = tri sur le total de la barre

	    List<String> sortedLabels = new ArrayList<>();
	    for (int idx : order) {
	        sortedLabels.add(labels.get(idx));
	    }

	    BarData data = new BarData().addLabels(sortedLabels.toArray(new String[0]));
	    for (Map.Entry<String, List<Long>> entry : valuesMap.entrySet()) {
	        BarDataset ds = new BarDataset().setLabel(entry.getKey());
	        for (int idx : order) {
	            ds.addData(entry.getValue().get(idx));
	        }
	        data.addDataset(ds);
	    }
	    BarOptions options = new BarOptions()
	            .setPlugins(new Plugins()
	                .setLegend(new LegendOptions().setDisplay(false)));
	    return withStackedOptions(new BarChart(data, options).toJson());
	}
    
    /** Indices des barres triés par valeur décroissante. keyDataset = null : total de la barre ; sinon valeur de ce dataset. */
    private static int[] orderDesc(int n, Map<String, List<Long>> valuesMap, String keyDataset) {
        long[] key = new long[n];
        for (Map.Entry<String, List<Long>> e : valuesMap.entrySet()) {
            if (keyDataset != null && !keyDataset.equals(e.getKey())) {
                continue;
            }
            List<Long> v = e.getValue();
            for (int i = 0; i < n; i++) {
                Long x = v.get(i);
                key[i] += (x == null ? 0L : x);
            }
        }
        return IntStream.range(0, n).boxed()
                .sorted((a, b) -> Long.compare(key[b], key[a]))   // tri stable : à égalité, l'ordre d'origine est conservé
                .mapToInt(Integer::intValue)
                .toArray();
    }

	public String getStats(String typeStats, String key, String param, String year){
		Context ctx = contextRepository.findByContextKey(key);
		String anneeUniv = (!"all".equals(year)) ? year : "20%";

		switch (typeStats) {
			case "sessionEpreuvesByCampus":
				return pieOrDoughnut(sessionEpreuveRepository.countSessionEpreuveByCampus(ctx.getId(), anneeUniv), false);
			case "presenceByContext":
				return pieOrDoughnut(tagCheckRepository.countPresenceByContext(ctx.getId(), anneeUniv), false);
			case "sessionEpreuveByYearMonth":
				return line(sessionEpreuveRepository.countSessionEpreuveByYearMonth(ctx.getId(), anneeUniv));
			case "countTagCheckByYearMonth":
				return line(tagCheckRepository.countTagCheckByYearMonth(ctx.getId(), anneeUniv));
			case "countTagChecksByTypeBadgeage":
				return pieOrDoughnut(tagCheckRepository.countTagChecksByTypeBadgeage(ctx.getId(), anneeUniv), true);
			case "countSessionEpreuveByType":
				return pieOrDoughnut(sessionEpreuveRepository.countSessionEpreuveByType(ctx.getId(), anneeUniv), false);
			default:
				return null;
		}
	}

	public String getStatsSuperAdmin(String typeStats, String year){
		String anneeUniv = (!"all".equals(year)) ? year : "20%";

		switch (typeStats) {
			case "sessionEpreuvesByContext":
				return stackedBar(sessionEpreuveRepository.countSessionEpreuvesParStatutByContext(anneeUniv));
			case "countTagChecksByContext":
				return stackedBar(tagCheckRepository.countPresenceParContext(anneeUniv));
			case "countSessionEpreuveByTypeByContext":
				return stackedBar(sessionEpreuveRepository.countSessionEpreuveByTypeByContext(anneeUniv));
			case "countTagChecksByTypeEmargementByContext":
				return stackedBar(tagCheckRepository.countTagChecksByTypeEmargementByContext(anneeUniv));
			case "countSessionsParMoisByContext":
				return lineByContext(sessionEpreuveRepository.countSessionsParMoisByContext(anneeUniv), 40);
			case "countBadgeagesParMoisByContext":
				return lineByContext(tagCheckRepository.countBadgeagesParMoisByContext(anneeUniv), 40);
			default:
				return null;
		}
	}
}
