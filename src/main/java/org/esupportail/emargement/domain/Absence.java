package org.esupportail.emargement.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Absence {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
	
	@ManyToOne
	@JsonIgnore
	private Context context;
	
	@ManyToOne
	private Person person;
	
	@ManyToOne
	private UserApp userApp;
	
	@ManyToOne
	private MotifAbsence motifAbsence;
	
    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;
    
	private @DateTimeFormat(pattern = "dd/MM/yyyy")
	Date dateModification;
    
	private @DateTimeFormat(pattern = "dd/MM/yyyy")
	Date dateDebut;
	
	private @DateTimeFormat(pattern = "dd/MM/yyyy")
	Date dateFin;
	
	private @DateTimeFormat(pattern = "HH:mm")
	Date heureDebut;
	
	private @DateTimeFormat(pattern = "HH:mm")
	Date heureFin;
	
    @Transient
    @JsonIgnore
    private List<MultipartFile> files;
    
    private @Transient
    Long nbStoredFiles;
    
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

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
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

	public Date getHeureDebut() {
		return heureDebut;
	}

	public void setHeureDebut(Date heureDebut) {
		this.heureDebut = heureDebut;
	}

	public Date getHeureFin() {
		return heureFin;
	}

	public void setHeureFin(Date heureFin) {
		this.heureFin = heureFin;
	}

	public List<MultipartFile> getFiles() {
		return files;
	}

	public void setFiles(List<MultipartFile> files) {
		this.files = files;
	}

	public UserApp getUserApp() {
		return userApp;
	}

	public void setUserApp(UserApp userApp) {
		this.userApp = userApp;
	}

	public Date getDateModification() {
		return dateModification;
	}

	public void setDateModification(Date dateModification) {
		this.dateModification = dateModification;
	}

	public Long getNbStoredFiles() {
		return nbStoredFiles;
	}

	public void setNbStoredFiles(Long nbStoredFiles) {
		this.nbStoredFiles = nbStoredFiles;
	}

	public MotifAbsence getMotifAbsence() {
		return motifAbsence;
	}

	public void setMotifAbsence(MotifAbsence motifAbsence) {
		this.motifAbsence = motifAbsence;
	}
}
