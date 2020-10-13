package org.esupportail.emargement.services;


import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.SessionEpreuve.TypeSessionEpreuve;
import org.esupportail.emargement.repositories.SessionEpreuveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;


@Service
public class ImportExportService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	SessionEpreuveRepository sessionEpreuveRepository;
	
	public  List<List<String>>  readAll(Reader reader) throws Exception {
		char separator = ';';
		List<List<String>> rows = new ArrayList<>();
		try {
			CSVReader csv = new CSVReaderBuilder(reader)
					.withCSVParser(new CSVParserBuilder().withSeparator(separator).build()).build();
			
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
		
		List<SessionEpreuve> se  = sessionEpreuveRepository.findSessionEpreuveByIsSessionEpreuveClosedFalseOrderByNomSessionEpreuve();
		if(!se.isEmpty()) {
			for (SessionEpreuve item : se) {
				if(!item.getIsSessionLibre()) {
					newSe.add(item);
				}
			}
		}
		return newSe;
	}

}
