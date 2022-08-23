package org.esupportail.emargement.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class AdeResourceBean {
	
	public Long eventId;
	
	public SessionEpreuve sessionEpreuve;
	
	public List<Map<Long,String>> instructors;
	
	public List<Map<Long,String>> classrooms;
	
	public List<Map<Long,String>> trainees;
	
	public List<Map<Long,String>> category6;
	
	public boolean isAlreadyimport;
	
	public Date lastUpdate;
	
	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public List<Map<Long, String>> getInstructors() {
		return instructors;
	}

	public void setInstructors(List<Map<Long, String>> instructors) {
		this.instructors = instructors;
	}

	public List<Map<Long, String>> getClassrooms() {
		return classrooms;
	}

	public void setClassrooms(List<Map<Long, String>> classrooms) {
		this.classrooms = classrooms;
	}

	public List<Map<Long, String>> getTrainees() {
		return trainees;
	}

	public void setTrainees(List<Map<Long, String>> trainees) {
		this.trainees = trainees;
	}

	public List<Map<Long, String>> getCategory6() {
		return category6;
	}

	public void setCategory6(List<Map<Long, String>> category6) {
		this.category6 = category6;
	}

	public SessionEpreuve getSessionEpreuve() {
		return sessionEpreuve;
	}

	public void setSessionEpreuve(SessionEpreuve sessionEpreuve) {
		this.sessionEpreuve = sessionEpreuve;
	}

	public boolean isAlreadyimport() {
		return isAlreadyimport;
	}

	public void setAlreadyimport(boolean isAlreadyimport) {
		this.isAlreadyimport = isAlreadyimport;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
}
