package org.esupportail.emargement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix="emargement.plans") 
public class PlanConfig {
	
	private String path;
	public String getPath() { 
		return path; 
	}
	public void setPath(String path) { 
		this.path = path; 
	}
}
