package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;
import org.esupportail.emargement.domain.Campus;
import org.esupportail.emargement.domain.Location;
import org.esupportail.emargement.repositories.CampusRepository;
import org.esupportail.emargement.repositories.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service 
public class CampusService {
	
	private final CampusRepository repository;
	private final LocationRepository locationRepository;
	@Autowired public CampusService(CampusRepository r, LocationRepository lR) {
		repository = r;
		locationRepository = lR;
	}
	public boolean existsById(Long id) {
		boolean result;
		try { result = repository.existsById(id); }
		catch(Throwable t) { result = false; }
		return result;
	}
	public Campus findById(Long id) {
		Campus c;
		try { c = repository.findById(id).orElse(null); }
		catch(Throwable e) { c = null; }
		return c;
	}
	public List<Campus> findAll() {
		List<Campus> campuses;
		try { campuses = repository.findAll(); }
		catch(Throwable e) { campuses = new ArrayList<Campus>(); }
		return campuses;
	}
	public List<Location> findLocations(Campus c) { return locationRepository.findByCampus(c); }
	public Page<Campus> findAll(Pageable p) {
		Page<Campus> page;
		try { page = repository.findAll(p); }
		catch(Throwable e) { page = Page.empty(p); }
		return page;
	}
}
