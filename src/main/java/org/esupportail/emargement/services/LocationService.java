package org.esupportail.emargement.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.esupportail.emargement.config.PlanConfig;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.domain.Plan;
import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.repositories.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.fortuna.ical4j.data.ParserException;

@Service 
public class LocationService {

	private static final Logger log = LoggerFactory.getLogger(LocationService.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final File file;
	private final Map<Long, Plan> plans;
	private final LocationRepository repository;
	private final EventService eventService;

	@Autowired
	LocationRepository locationRepository;

	@Autowired 
	public LocationService(PlanConfig conf, LocationRepository r, EventService eS) throws StreamReadException, DatabindException, IOException {
		Path path;
		File file;
		Map<Long, Plan> plans;
		path = Paths.get(conf.getPath()).toAbsolutePath().normalize();
		file = path.toFile();
		if(file.exists()) plans = mapper.readValue(file, new TypeReference<Map<Long,Plan>>(){}); // 
		else {
			Files.write(Files.createFile(path), "{}".getBytes());
			plans = new HashMap<>();
		}
		this.file = file;
		this.plans = plans;
		repository = r;
		eventService = eS;
	}

	public boolean existsById(Long id) {
		boolean existing;
		try { existing = repository.existsById(id); }
		catch(Throwable e) { existing = false; }
		return existing;
	}
	public boolean existsPlan(Long id) { return plans.containsKey(id); }

	@Transactional(rollbackFor = Exception.class) 
	public byte save(Location loc) throws Exception {
		byte result;
		Long id = loc.getId();
		
		System.out.println("id ==================== " + id);
		if(id == null || !repository.existsSessionByLocation_IdAndStatutNotClosedAndCapacityGreaterThan(id, loc.getCapacite())) {
			String nom = loc.getNom();
			if(!repository.existsByNom(nom) || repository.findByNom(nom).get().getId().equals(id)) {
				repository.save(loc);
				if(loc.hasPlan()) {
					plans.put(loc.getId(), loc.getPlan());
					mapper.writeValue(file, plans);
				}
				else if(plans.remove(loc.getId()) != null) mapper.writeValue(file, plans);;
				result = 0;
			}
			else result = 1;
		}
		else result = 2;
		return result;
	}
	@Transactional(rollbackFor = { Throwable.class }) 
	public byte delete(Long id) throws Throwable {
		byte result;
		if(!repository.existsSessionByLocation_IdAndStatutNotClosed(id)) {
			if(plans.remove(id) != null) mapper.writeValue(file, plans);
			repository.deleteById(id);
			result = 0;
		}
		else result = 1;
		return result;
	}
	public Long count() { return (Long)repository.count(); }
	public Location findById(boolean shouldFindPlan, Long id) {
		Location loc;
		try {
			loc = repository.findById(id).orElse(null);
			if(shouldFindPlan && loc != null && plans.containsKey(id)) loc.setPlan(plans.get(id));
		}
		catch(Throwable e) { loc = null; }
		return loc;
	}
	public Location findByNom(String nom) { return repository.findByNom(nom).orElse(null); }
	public Plan findPlan(Long id) { return plans.get(id); }

	public List<String> getSuggestedLocation() throws IOException, ParserException {

		List<String> locationsFromIcs = eventService.getLocationsFromICs(eventService.getAllUrlList());
		List<String> locations =  locationRepository.findAll().stream().map(l -> l.getNom().trim()).collect(Collectors.toList());
		locationsFromIcs.removeAll(locations);
		return locationsFromIcs;
	}


	public List<SessionEpreuve> findSessions(Long id) { return repository.findSessionByLocation_IdAndStatutNotClosed(id); }
	public Page<Location> findByNomStartsWith(String nom, Pageable p) {
		Page<Location> page;
		try {
			page = repository.findByNom(nom, p);
			page.forEach(location -> location.setPlan(findPlan(location.getId())));
		}
		catch(Throwable e) { page = Page.empty(p); }
		return page;
	}
	public Page<Location> findAll(Pageable p) {
		Page<Location> page;
		try {
			page = repository.findAll(p);
			page.forEach(location -> location.setPlan(findPlan(location.getId())));
		}
		catch(Throwable e) { page = Page.empty(); }
		return page;
	}



}
