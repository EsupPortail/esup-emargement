package org.esupportail.emargement.domain;

import javax.persistence.Column;
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

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class MotifAbsence {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
	
	@ManyToOne
	@JsonIgnore
	private Context context;
	
	public static enum TypeAbsence {
	       ABSENCE, RETARD
	}
	
	public static enum StatutAbsence {
	       JUSTIFIE, INJUSTIFIE
	}
	
	private String libelle;
	
	@Column
	@Enumerated(EnumType.STRING)
	private TypeAbsence typeAbsence;
	
	@Column
	@Enumerated(EnumType.STRING)
	private StatutAbsence statutAbsence;
	
	private Boolean isActif;

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

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public TypeAbsence getTypeAbsence() {
		return typeAbsence;
	}

	public void setTypeAbsence(TypeAbsence typeAbsence) {
		this.typeAbsence = typeAbsence;
	}

	public StatutAbsence getStatutAbsence() {
		return statutAbsence;
	}

	public void setStatutAbsence(StatutAbsence statutAbsence) {
		this.statutAbsence = statutAbsence;
	}

	public Boolean getIsActif() {
		return isActif;
	}

	public void setIsActif(Boolean isActif) {
		this.isActif = isActif;
	}
}
