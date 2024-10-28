package org.esupportail.emargement.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class Task {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
    private Long id;
	
	@ManyToOne
	@JsonIgnore
	private Context context;
	
    public static enum Status {
        NOTASK, INPROGRESS, ENDED, FAILED
      }
	
    @Enumerated(EnumType.STRING)
	private Status status;
	
	private String param;
	
	private String libelle;
	
	private String adeProject;
	
    @DateTimeFormat(pattern = "dd/MM/yyyy")
	private Date dateCreation;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
	private Date dateExecution;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
	private Date dateFinExecution;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
	private Date dateDebut;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
	private Date dateFin;
	
	private int nbModifs;
	
	private int nbItems;
	
    @ManyToOne
	private Campus campus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public int getNbModifs() {
		return nbModifs;
	}

	public void setNbModifs(int nbModifs) {
		this.nbModifs = nbModifs;
	}

	public Date getDateExecution() {
		return dateExecution;
	}

	public void setDateExecution(Date dateExecution) {
		this.dateExecution = dateExecution;
	}

	public Campus getCampus() {
		return campus;
	}

	public void setCampus(Campus campus) {
		this.campus = campus;
	}

	public String getAdeProject() {
		return adeProject;
	}

	public void setAdeProject(String adeProject) {
		this.adeProject = adeProject;
	}

	public Date getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}

	public int getNbItems() {
		return nbItems;
	}

	public void setNbItems(int nbItems) {
		this.nbItems = nbItems;
	}

	public Date getDateFinExecution() {
		return dateFinExecution;
	}

	public void setDateFinExecution(Date dateFinExecution) {
		this.dateFinExecution = dateFinExecution;
	}
}
