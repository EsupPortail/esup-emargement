package org.esupportail.emargement.beans;

import java.util.List;

import org.esupportail.emargement.domain.SessionLocation;
import org.esupportail.emargement.domain.TagCheck;

public class UpdatePresenceResult {

    private List<TagCheck> list;

    private TagCheck presentTagCheck;

    private float percent;

    private Long totalPresent;

    private SessionLocation sessionLocationBadged;

    private String msgError;
    
    public boolean hasEmitterData() {
        return presentTagCheck != null;
    }

	public List<TagCheck> getList() {
		return list;
	}

	public void setList(List<TagCheck> list) {
		this.list = list;
	}

	public TagCheck getPresentTagCheck() {
		return presentTagCheck;
	}

	public void setPresentTagCheck(TagCheck presentTagCheck) {
		this.presentTagCheck = presentTagCheck;
	}

	public float getPercent() {
		return percent;
	}

	public void setPercent(float percent) {
		this.percent = percent;
	}

	public Long getTotalPresent() {
		return totalPresent;
	}

	public void setTotalPresent(Long totalPresent) {
		this.totalPresent = totalPresent;
	}

	public SessionLocation getSessionLocationBadged() {
		return sessionLocationBadged;
	}

	public void setSessionLocationBadged(SessionLocation sessionLocationBadged) {
		this.sessionLocationBadged = sessionLocationBadged;
	}

	public String getMsgError() {
		return msgError;
	}

	public void setMsgError(String msgError) {
		this.msgError = msgError;
	}
}