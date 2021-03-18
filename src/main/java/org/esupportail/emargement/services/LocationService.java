package org.esupportail.emargement.services;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.repositories.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.fortuna.ical4j.data.ParserException;

@Service
public class LocationService {
	
	@Autowired
	LocationRepository locationRepository;
	
	@Resource
	EventService eventService;
	
	public List<String> getSuggestedLocation() throws IOException, ParserException {
		
		List<String> locationsFromIcs = eventService.getLocationsFromICs(eventService.getAllUrlList());
		List<String> locations =  locationRepository.findAll().stream().map(l -> l.getNom().trim()).collect(Collectors.toList());
		locationsFromIcs.removeAll(locations);
		return locationsFromIcs;
	}
}
