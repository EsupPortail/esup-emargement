package org.esupportail.emargement.services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.StoredFile;
import org.esupportail.emargement.repositories.LocationRepository;
import org.esupportail.emargement.repositories.StoredFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.fortuna.ical4j.data.ParserException;

@Service
public class LocationService {
	
	@Autowired
	LocationRepository locationRepository;
	
	@Autowired
	StoredFileRepository storedFileRepository;
	
	@Resource
	StoredFileService storedFileService;
	
	@Resource
	EventService eventService;
	
	public void save(Location location, String emargementContext ) throws IOException {
		
		StoredFile sf = null;
		StoredFile oldSf =  null;
		boolean deleteOldPlan =false;
		if(location.getId()!=null){
			Optional<Location> loc = locationRepository.findById(location.getId());
			oldSf = loc.get().getPlan();
		}
		if(location.getFile().getSize()>0) {
			  sf = storedFileService.setStoredFile(new StoredFile(), location.getFile(), emargementContext);
			  deleteOldPlan = true;
		}else {
			sf = oldSf;
		}
		
		location.setPlan(sf);
		locationRepository.save(location);
		if(deleteOldPlan) {
			if(oldSf!=null) {
				storedFileRepository.delete(oldSf);
			}
		}
	}
	
	public List<String> getSuggestedLocation() throws IOException, ParserException {
		
		List<String> locationsFromIcs = eventService.getLocationsFromICs(eventService.getAllUrlList());
		List<String> locations =  locationRepository.findAll().stream().map(l -> l.getNom().trim()).collect(Collectors.toList());
		locationsFromIcs.removeAll(locations);
		return locationsFromIcs;
	}
}
