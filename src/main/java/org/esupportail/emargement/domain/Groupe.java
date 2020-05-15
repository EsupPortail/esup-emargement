package org.esupportail.emargement.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class Groupe implements ContextSupport {
	
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
	@ManyToOne
	private Context context;

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}
	
	public String description;
	
	public String nom;
	
	@Transient
	public long nbTagCheck; 
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	public Date dateCreation;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	public Date dateModification;
	
	public String modificateur;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public Date getDateModification() {
		return dateModification;
	}

	public void setDateModification(Date dateModification) {
		this.dateModification = dateModification;
	}

	public String getModificateur() {
		return modificateur;
	}

	public void setModificateur(String modificateur) {
		this.modificateur = modificateur;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getNbTagCheck() {
		return nbTagCheck;
	}

	public void setNbTagCheck(long nbTagCheck) {
		this.nbTagCheck = nbTagCheck;
	}
	
}
