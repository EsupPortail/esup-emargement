package org.esupportail.emargement.domain;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.format.annotation.NumberFormat;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class SessionLocation implements ContextSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@ManyToOne
	private Context context;
	
    @NumberFormat
    private int priorite;

    @NumberFormat
    private int capacite;

    @ManyToOne
    private Location location;

    @ManyToOne
    private SessionEpreuve sessionEpreuve;
    
    @Transient
    public Long nbInscritsSessionLocation= Long.valueOf("0");
    
    @Transient
    public Long nbPresentsSessionLocation= Long.valueOf("0");
    
    @Transient
    private Long tauxRemplissage = Long.valueOf("0");
    
    private Boolean isTiersTempsOnly = false;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getPriorite() {
		return priorite;
	}

	public void setPriorite(int priorite) {
		this.priorite = priorite;
	}

	public int getCapacite() {
		return capacite;
	}

	public void setCapacite(int capacite) {
		this.capacite = capacite;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public SessionEpreuve getSessionEpreuve() {
		return sessionEpreuve;
	}

	public void setSessionEpreuve(SessionEpreuve sessionEpreuve) {
		this.sessionEpreuve = sessionEpreuve;
	}

	public Long getNbInscritsSessionLocation() {
		return nbInscritsSessionLocation;
	}

	public void setNbInscritsSessionLocation(Long nbInscritsSessionLocation) {
		this.nbInscritsSessionLocation = nbInscritsSessionLocation;
	}

	public Long getTauxRemplissage() {
		return tauxRemplissage;
	}

	public void setTauxRemplissage(Long tauxRemplissage) {
		this.tauxRemplissage = tauxRemplissage;
	}

	public Boolean getIsTiersTempsOnly() {
		return isTiersTempsOnly;
	}

	public void setIsTiersTempsOnly(Boolean isTiersTempsOnly) {
		this.isTiersTempsOnly = isTiersTempsOnly;
	}
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Long getNbPresentsSessionLocation() {
		return nbPresentsSessionLocation;
	}

	public void setNbPresentsSessionLocation(Long nbPresentsSessionLocation) {
		this.nbPresentsSessionLocation = nbPresentsSessionLocation;
	}
	
   }
