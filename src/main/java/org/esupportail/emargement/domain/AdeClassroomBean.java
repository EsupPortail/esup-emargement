package org.esupportail.emargement.domain;

import java.util.Date;

public class AdeClassroomBean {
	
	public Long idClassRoom;
	
	public String nom;
	
	public String chemin;
	
	public String type;
	
	public int size;
	
	public Date lastUpdate;
	
	public boolean isAlreadyimport;

	public Long getIdClassRoom() {
		return idClassRoom;
	}

	public void setIdClassRoom(Long idClassRoom) {
		this.idClassRoom = idClassRoom;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getChemin() {
		return chemin;
	}

	public void setChemin(String chemin) {
		this.chemin = chemin;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public boolean isAlreadyimport() {
		return isAlreadyimport;
	}

	public void setAlreadyimport(boolean isAlreadyimport) {
		this.isAlreadyimport = isAlreadyimport;
	}
}
