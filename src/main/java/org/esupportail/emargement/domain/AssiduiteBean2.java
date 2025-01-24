package org.esupportail.emargement.domain;

import java.util.Date;

public class AssiduiteBean2 {
	
	Date dateDebut;
	
	Date dateFin;
	
	String strDateDebut;
	
	String strDateFin;
	
	String type;
	
	String motifType;
	
	String motifStatut;
	
	String codeEtu;
	
	Groupe groupe;
	
	String nom;
	
	String searchField;
	
	String searchValue;
	
	String situation;
	
	String eppn;

	public Date getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMotifType() {
		return motifType;
	}

	public void setMotifType(String motifType) {
		this.motifType = motifType;
	}

	public String getMotifStatut() {
		return motifStatut;
	}

	public void setMotifStatut(String motifStatut) {
		this.motifStatut = motifStatut;
	}

	public String getSearchField() {
		return searchField;
	}

	public void setSearchField(String searchField) {
		this.searchField = searchField;
	}

	public String getSearchValue() {
		return searchValue;
	}

	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}

	public String getStrDateDebut() {
		return strDateDebut;
	}

	public void setStrDateDebut(String strDateDebut) {
		this.strDateDebut = strDateDebut;
	}

	public String getStrDateFin() {
		return strDateFin;
	}

	public void setStrDateFin(String strDateFin) {
		this.strDateFin = strDateFin;
	}

	public String getCodeEtu() {
		return codeEtu;
	}

	public void setCodeEtu(String codeEtu) {
		this.codeEtu = codeEtu;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getSituation() {
		return situation;
	}

	public void setSituation(String situation) {
		this.situation = situation;
	}

	public String getEppn() {
		return eppn;
	}

	public void setEppn(String eppn) {
		this.eppn = eppn;
	}

	public Groupe getGroupe() {
		return groupe;
	}

	public void setGroupe(Groupe groupe) {
		this.groupe = groupe;
	}
}
