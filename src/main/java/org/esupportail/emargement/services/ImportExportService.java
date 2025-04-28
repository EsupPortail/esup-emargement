package org.esupportail.emargement.services;


import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.Statut;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.esupportail.emargement.web.manager.ExtractionController.ExtractionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;


@Service
public class ImportExportService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	@Resource
	AppliConfigService appliConfigService;
	
	@Autowired(required = false)
	ApogeeService apogeeService;
	
	public  List<List<String>>  readAll(Reader reader) throws Exception {
		List<List<String>> rows = new ArrayList<>();
		try {
		    CSVReader csv = new CSVReaderBuilder(reader).build(); // Uses default ',' separator
		    
		    for (String[] nextLine : csv) {
		        rows.add(new ArrayList<>(Arrays.asList(nextLine)));
		    }
		    csv.close();
		} catch (Exception e) {
		    rows = null;
		    log.error("Csv mal formé", e);
		}
		return rows;
	}
	
	public List<String> getYearsUntilNow() {

		List<String> years = new ArrayList<String>();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		if(month<8) {
			year = year-1;
		}
		final int start = year-1;
		
		for(int i= start;i<=year; i++) {
			years.add(String.valueOf(i));
		}
		Collections.reverse(years);
		return years;
	}
	
	public List<SessionEpreuve> getNotFreeSessionEpreuve(){
		List<SessionEpreuve> newSe = new LinkedList<SessionEpreuve>();
		Statut statuts [] = {Statut.CLOSED, Statut.CANCELLED};
		List<SessionEpreuve> se  = sessionEpreuveRepository.findSessionEpreuveByStatutNotInOrderByDateExamen(Arrays.asList(statuts));
		if(!se.isEmpty()) {
			for (SessionEpreuve item : se) {
				newSe.add(item);
			}
		}
		return newSe;
	}
	public ExtractionType getDisplayType(ExtractionType type) {
		if (type == null) {
			String searchChars = "ALCG";
			Character firstMatch = appliConfigService.getListImportExport().chars()
					.mapToObj(c -> (char) c)
					.filter(c -> searchChars.indexOf(c) != -1)
					.findFirst() 
					.orElse(null); 
			if (firstMatch != null) {
				if (firstMatch == 'A' && apogeeService != null) {
					type = ExtractionType.apogee;
				} else if (firstMatch == 'L') {
					type = ExtractionType.ldap;
				} else if (firstMatch == 'C') {
					type = ExtractionType.csv;
				} else if (firstMatch == 'G') {
					type = ExtractionType.groupes;
				} 
			}
		}
		return type;
	}
}
