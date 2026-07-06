package org.esupportail.emargement.beans;

import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagChecker;

public class IsTagableContext {
	
	private SessionLocation sessionLocation;
	private TagChecker tagChecker;
	private boolean isSessionLibre;
    
	public IsTagableContext(SessionLocation sessionLocation, TagChecker tagChecker, boolean isSessionLibre) {
		this.sessionLocation = sessionLocation;
		this.tagChecker = tagChecker;
		this.isSessionLibre = isSessionLibre;
	}

	public SessionLocation getSessionLocation() {
		return sessionLocation;
	}

	public void setSessionLocation(SessionLocation sessionLocation) {
		this.sessionLocation = sessionLocation;
	}

	public TagChecker getTagChecker() {
		return tagChecker;
	}

	public void setTagChecker(TagChecker tagChecker) {
		this.tagChecker = tagChecker;
	}

	public boolean isSessionLibre() {
		return isSessionLibre;
	}

	public void setSessionLibre(boolean isSessionLibre) {
		this.isSessionLibre = isSessionLibre;
	}
}
