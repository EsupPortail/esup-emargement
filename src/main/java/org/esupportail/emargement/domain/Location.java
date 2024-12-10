package org.esupportail.emargement.domain;

import javax.persistence.Column;
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
public class Location implements ContextSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne
	private Context context;

	private String nom;

	@ManyToOne
	private Campus campus;

	@NumberFormat
	private int capacite=0;

	@Transient 
	private Plan plan;

	@Column(columnDefinition = "TEXT")
	private String adresse;

	private Long adeClassRoomId;

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

	public Campus getCampus() {
		return campus;
	}

	public void setCampus(Campus campus) {
		this.campus = campus;
	}

	public int getCapacite() {
		return capacite;
	}

	public void setCapacite(int capacite) {
		this.capacite = capacite;
	}

	public String getAdresse() {
		return adresse;
	}

	public void setAdresse(String adresse) {
		this.adresse = adresse;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Long getAdeClassRoomId() {
		return adeClassRoomId;
	}

	public void setAdeClassRoomId(Long adeClassRoomId) {
		this.adeClassRoomId = adeClassRoomId;
	}

	public void setPlan(Plan plan) { 
		this.plan = plan;
	}

	public Plan getPlan() { return plan; }

	public boolean hasPlan() { 
		return plan != null; 
	}
}
