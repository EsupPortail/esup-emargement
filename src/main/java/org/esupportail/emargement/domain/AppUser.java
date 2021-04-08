package org.esupportail.emargement.domain;

public class AppUser {
	
	public Long personOrGuestId;
	
	public String eppnOrEmail;
	
	public String type;
	
	public String nom;
	
	public String prenom;
	
	public String numEtu;
	
	public int nbGroupes;
	
	public int nbSessions;
	

	public Long getPersonOrGuestId() {
		return personOrGuestId;
	}

	public void setPersonOrGuestId(Long personOrGuestId) {
		this.personOrGuestId = personOrGuestId;
	}

	public String getEppnOrEmail() {
		return eppnOrEmail;
	}

	public void setEppnOrEmail(String eppnOrEmail) {
		this.eppnOrEmail = eppnOrEmail;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getNumEtu() {
		return numEtu;
	}

	public void setNumEtu(String numEtu) {
		this.numEtu = numEtu;
	}

	public int getNbGroupes() {
		return nbGroupes;
	}

	public void setNbGroupes(int nbGroupes) {
		this.nbGroupes = nbGroupes;
	}

	public int getNbSessions() {
		return nbSessions;
	}

	public void setNbSessions(int nbSessions) {
		this.nbSessions = nbSessions;
	}
	
}
