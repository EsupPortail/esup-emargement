package org.esupportail.emargement.domain;

public class CalendarDTO {
	private Long id;
    private String title;
    private String start;
    private String end;
    private String color;
    private String url;
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
 
}