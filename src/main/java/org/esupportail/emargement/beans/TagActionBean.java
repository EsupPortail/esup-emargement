package org.esupportail.emargement.beans;

import java.util.Date;

import org.esupportail.emargement.domain.Context;
import org.esupportail.emargement.domain.SessionEpreuve;

public class TagActionBean {

	private String eppn;
	private String eppnInit;
	private String nomSalle;
	private Long sessionId;

	private SessionEpreuve sessionEpreuve;
	private Context context;

	private Date date;
	private Date dateFin;

	private boolean isBlackListed;
	private boolean isSessionLibre;
	private boolean isTagCheckerTagged;
	
	public TagActionBean(String eppn, String eppnInit, String nomSalle, Long sessionId, SessionEpreuve sessionEpreuve, Context context, Date date, Date dateFin, boolean isBlackListed, boolean isSessionLibre, boolean isTagCheckerTagged) {
		this.eppn = eppn;
		this.eppnInit = eppnInit;
		this.nomSalle = nomSalle;
		this.sessionId = sessionId;
		this.sessionEpreuve = sessionEpreuve;
		this.context = context;
		this.date = date;
		this.dateFin = dateFin;
		this.isBlackListed = isBlackListed;
		this.isSessionLibre = isSessionLibre;
		this.isTagCheckerTagged = isTagCheckerTagged;
	}

	public String getEppn() {
		return eppn;
	}

	public void setEppn(String eppn) {
		this.eppn = eppn;
	}

	public String getEppnInit() {
		return eppnInit;
	}

	public void setEppnInit(String eppnInit) {
		this.eppnInit = eppnInit;
	}

	public String getNomSalle() {
		return nomSalle;
	}

	public void setNomSalle(String nomSalle) {
		this.nomSalle = nomSalle;
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public SessionEpreuve getSessionEpreuve() {
		return sessionEpreuve;
	}

	public void setSessionEpreuve(SessionEpreuve sessionEpreuve) {
		this.sessionEpreuve = sessionEpreuve;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}

	public boolean isBlackListed() {
		return isBlackListed;
	}

	public void setBlackListed(boolean isBlackListed) {
		this.isBlackListed = isBlackListed;
	}

	public boolean isSessionLibre() {
		return isSessionLibre;
	}

	public void setSessionLibre(boolean isSessionLibre) {
		this.isSessionLibre = isSessionLibre;
	}

	public boolean isTagCheckerTagged() {
		return isTagCheckerTagged;
	}

	public void setTagCheckerTagged(boolean isTagCheckerTagged) {
		this.isTagCheckerTagged = isTagCheckerTagged;
	}
}