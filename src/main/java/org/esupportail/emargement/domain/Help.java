package org.esupportail.emargement.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Help {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
	
    @Column(name = "key", length = 120, unique = true)
    private String key;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
	private @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
	Date dateModification;
	
    @Column(columnDefinition = "TEXT")
    private String title;
	
    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(columnDefinition = "TEXT")
    private String actions;

    @Column(columnDefinition = "TEXT")
    private String important;

    @Column(columnDefinition = "TEXT")
    private String warning;
    
    private Integer version;
    
    public List<String> getActionsList() {
    	if (actions == null || actions.isBlank()) {
    		return Collections.emptyList();
    	}
    	return Arrays.stream(actions.split("\\|"))
    			.map(String::trim)
    			.filter(StringUtils::isNotBlank)
    			.collect(Collectors.toList());
    }
    
    public List<String> getImportantList() {
    	if (important == null || important.isBlank()) {
    		return Collections.emptyList();
    	}
    	return Arrays.stream(important.split("\\|"))
    			.map(String::trim)
    			.filter(StringUtils::isNotBlank)
    			.collect(Collectors.toList());
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDateModification() {
		return dateModification;
	}

	public void setDateModification(Date dateModification) {
		this.dateModification = dateModification;
	}

	public String getIntro() {
		return intro;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public String getActions() {
		return actions;
	}

	public void setActions(String actions) {
		this.actions = actions;
	}

	public String getImportant() {
		return important;
	}

	public void setImportant(String important) {
		this.important = important;
	}

	public String getWarning() {
		return warning;
	}

	public void setWarning(String warning) {
		this.warning = warning;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
