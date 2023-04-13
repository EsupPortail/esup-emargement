package org.esupportail.emargement.domain;

import java.util.Map;

public class AssiduiteBean {
	
	String anneeUniv;
	
	Long totalSession;
	
	Long nbPresent;
	
	String percentPresent;
	
	Map<String, String> detailPerence;

	public String getAnneeUniv() {
		return anneeUniv;
	}

	public void setAnneeUniv(String anneeUniv) {
		this.anneeUniv = anneeUniv;
	}

	public Long getTotalSession() {
		return totalSession;
	}

	public void setTotalSession(Long totalSession) {
		this.totalSession = totalSession;
	}

	public Long getNbPresent() {
		return nbPresent;
	}

	public void setNbPresent(Long nbPresent) {
		this.nbPresent = nbPresent;
	}
	
	public String getPercentPresent() {
		return percentPresent;
	}

	public void setPercentPresent(String percentPresent) {
		this.percentPresent = percentPresent;
	}

	public Map<String, String> getDetailPerence() {
		return detailPerence;
	}

	public void setDetailPerence(Map<String, String> detailPerence) {
		this.detailPerence = detailPerence;
	}
}
