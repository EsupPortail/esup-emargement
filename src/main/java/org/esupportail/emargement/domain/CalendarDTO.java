package org.esupportail.emargement.domain;

import java.util.Map;

public class CalendarDTO {
	private Long id;
    private String title;
    private String start;
    private String end;
    private String color;
    private String url;
    private String textColor;
    private Boolean allDay = false;
    private String[] classNames;
    private Map<String, Object> extendedProps;
    private String professeur;
    private String salle;
    private String site;
    private String typeSession;
    private boolean qrCodeAvailable;
    private String qrEppn;
    private Long qrSession;   // id de sessionLocationExpected
    
    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
    }
    public Long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getStart() {
        return start;
    }
    public void setStart(String start) {
        this.start = start;
    }
    public String getEnd() {
        return end;
    }
    public void setEnd(String end) {
        this.end = end;
    }
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTextColor() {
		return textColor;
	}
	public void setTextColor(String textColor) {
		this.textColor = textColor;
	}
	public Boolean getAllDay() {
		return allDay;
	}
	public void setAllDay(Boolean allDay) {
		this.allDay = allDay;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String[] getClassNames() {
		return classNames;
	}
	public void setClassNames(String[] classNames) {
		this.classNames = classNames;
	}
	public Map<String, Object> getExtendedProps() {
		return extendedProps;
	}
	public void setExtendedProps(Map<String, Object> extendedProps) {
		this.extendedProps = extendedProps;
	}
	public String getProfesseur() {
		return professeur;
	}
	public void setProfesseur(String professeur) {
		this.professeur = professeur;
	}
	public String getSalle() {
		return salle;
	}
	public void setSalle(String salle) {
		this.salle = salle;
	}
	public String getSite() {
		return site;
	}
	public void setSite(String site) {
		this.site = site;
	}
	public String getTypeSession() {
		return typeSession;
	}
	public void setTypeSession(String typeSession) {
		this.typeSession = typeSession;
	}
	public boolean isQrCodeAvailable() {
		return qrCodeAvailable;
	}
	public void setQrCodeAvailable(boolean qrCodeAvailable) {
		this.qrCodeAvailable = qrCodeAvailable;
	}
	public String getQrEppn() {
		return qrEppn;
	}
	public void setQrEppn(String qrEppn) {
		this.qrEppn = qrEppn;
	}
	public Long getQrSession() {
		return qrSession;
	}
	public void setQrSession(Long qrSession) {
		this.qrSession = qrSession;
	}
	
}