package org.esupportail.emargement.domain;

import java.util.Date;

public class Archive {
	
	public String anneeUniv;
	
	public Long nbSessions;
	
	public Long nbTagChecks;
	
	public Long nbAnonymousTagChecks;
	
	public StoredFile file;
	
	public Date firstDate;
	
	public Date LastDate;
	
	public Date dateArchivage;
	
	public String loginArchivage;

	public String getAnneeUniv() {
		return anneeUniv;
	}

	public void setAnneeUniv(String anneeUniv) {
		this.anneeUniv = anneeUniv;
	}

	public Long getNbSessions() {
		return nbSessions;
	}

	public void setNbSessions(Long nbSessions) {
		this.nbSessions = nbSessions;
	}

	public Long getNbTagChecks() {
		return nbTagChecks;
	}

	public void setNbTagChecks(Long nbTagChecks) {
		this.nbTagChecks = nbTagChecks;
	}

	public StoredFile getFile() {
		return file;
	}

	public void setFile(StoredFile file) {
		this.file = file;
	}

	public Date getFirstDate() {
		return firstDate;
	}

	public void setFirstDate(Date firstDate) {
		this.firstDate = firstDate;
	}

	public Date getLastDate() {
		return LastDate;
	}

	public void setLastDate(Date lastDate) {
		LastDate = lastDate;
	}

	public Date getDateArchivage() {
		return dateArchivage;
	}

	public void setDateArchivage(Date dateArchivage) {
		this.dateArchivage = dateArchivage;
	}

	public String getLoginArchivage() {
		return loginArchivage;
	}

	public void setLoginArchivage(String loginArchivage) {
		this.loginArchivage = loginArchivage;
	}

	public Long getNbAnonymousTagChecks() {
		return nbAnonymousTagChecks;
	}

	public void setNbAnonymousTagChecks(Long nbAnonymousTagChecks) {
		this.nbAnonymousTagChecks = nbAnonymousTagChecks;
	}
	
}
