package org.esupportail.emargement.beans;

import org.esupportail.emargement.domain.SessionEpreuve;
import org.springframework.data.domain.Page;

public class SessionEpreuveResult {
	
	private Page<SessionEpreuve> page;
    private String dateSessions;
    private String view;
    
	public Page<SessionEpreuve> getPage() {
		return page;
	}
	public void setPage(Page<SessionEpreuve> page) {
		this.page = page;
	}
	public String getDateSessions() {
		return dateSessions;
	}
	public void setDateSessions(String dateSessions) {
		this.dateSessions = dateSessions;
	}
	public String getView() {
		return view;
	}
	public void setView(String view) {
		this.view = view;
	}
    
}
