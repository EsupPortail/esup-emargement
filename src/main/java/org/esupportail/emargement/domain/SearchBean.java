package org.esupportail.emargement.domain;

public class SearchBean {
	
	String nom;
	
	String prenom;
	
	String identifiant;
	
	String typeObject;
	
	Long id;

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

	public String getIdentifiant() {
		return identifiant;
	}

	public void setIdentifiant(String identifiant) {
		this.identifiant = identifiant;
	}

	public String getTypeObject() {
		return typeObject;
	}

	public void setTypeObject(String typeObject) {
		this.typeObject = typeObject;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}