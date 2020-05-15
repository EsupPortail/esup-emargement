package org.esupportail.emargement.services;

import java.util.ArrayList;
import java.util.List;

import org.esupportail.emargement.domain.Groupe;
import org.esupportail.emargement.repositories.GroupeRepository;
import org.esupportail.emargement.repositories.TagCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class GroupeService {
	
	@Autowired
	TagCheckRepository tagCheckRepository;
	
	@Autowired
	GroupeRepository groupeRepository;
	
	public Long getNbTagChecksInGroup(Long id) {
		Long count = new Long(0);
		
		count = tagCheckRepository.countTagCheckByGroupeId(id);
		
		return count;
	}
	
	public void computeCounters(List<Groupe> groupes) {
		
		for(Groupe groupe : groupes) {
			Long count = getNbTagChecksInGroup(groupe.getId());
			groupe.setNbTagCheck(count);
		}
	}
	
	public List<Groupe> getNotEmptyGroupes(){
		
		List<Groupe> groupes =  groupeRepository.findAll(Sort.by(Sort.Direction.ASC, "nom"));
		List<Groupe> notEmptyGroupes = new ArrayList<Groupe>();
		if(!groupes.isEmpty()) {
			for(Groupe groupe : groupes) {
				Long count = getNbTagChecksInGroup(groupe.getId());
				if(count>0) {
					groupe.setNbTagCheck(count);
					notEmptyGroupes.add(groupe);
				}
			}
		}
		
		return notEmptyGroupes;
	}
}
