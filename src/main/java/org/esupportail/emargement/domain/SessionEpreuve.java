package org.esupportail.emargement.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class SessionEpreuve implements ContextSupport {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
	
	@ManyToOne
	@JsonIgnore
	private Context context;
	
    private String nomSessionEpreuve;
    
    @Column
    @Enumerated(EnumType.STRING)
    public TypeBadgeage typeBadgeage;
    
    public static enum TypeBadgeage {
       SALLE, SESSION
    };
    
    public static enum Statut {
       OPENED, STANDBY, CLOSED, CANCELLED
     };
     
	@Column
	@Enumerated(EnumType.STRING)
	public Statut statut;
    
    @ManyToOne
    private TypeSession typeSession;
    
    @ManyToOne
    private Campus campus;
    
    private String anneeUniv;
    
    private Boolean isSecondTag = false;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateExamen;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateFin;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateCreation;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateImport;
    
    @DateTimeFormat(pattern = "HH:mm")
    @Temporal(TemporalType.TIME)
    private Date heureConvocation;

    @DateTimeFormat(pattern = "HH:mm")
    @Temporal(TemporalType.TIME)
    private Date heureEpreuve;
    
    @DateTimeFormat(pattern = "HH:mm")
    @Temporal(TemporalType.TIME)
    private Date finEpreuve;
    
    public Boolean isProcurationEnabled = false;
    
    public Boolean isSessionLibre = false;
    
    @JsonIgnore
    public Boolean isSaveInExcluded = false;
    
    @JsonIgnore
    public Boolean isGroupeDisplayed = false;
    
    @JsonIgnore
    public Integer maxBadgeageAlert = 1;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateArchivage;
    
    @JsonIgnore
    public String loginArchivage;

    private @Transient
    String dureeEpreuve;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    private @Transient
    Long nbLieuxSession;
    
    private @Transient
    Long nbInscritsSession;
    
    private @Transient
    Long nbTagCheckerSession;
    
    private @Transient
    Long nbPresentsSession;
    
    private @Transient
    Long nbDispatchTagCheck;
    
    private @Transient
    Long nbCheckedByCardTagCheck;
    
    private @Transient
    Long nbUnknown;
    
    private @Transient
    Long nbStoredFiles;
    
    private @Transient
    String nbHours;
    
    @Transient
    @JsonIgnore
    private List<MultipartFile> files;
    
    @ManyToOne
    public Groupe blackListGroupe;
    
    private Long adeEventId;
    
    private Long adeProjectId;
    
    public boolean isSessionEpreuveClosed() {
    	if(Statut.CLOSED.equals(getStatut())){
    		return true;
    	}
		return false;
    }
    
	public Groupe getBlackListGroupe() {
		return blackListGroupe;
	}

	public void setBlackListGroupe(Groupe blackListGroupe) {
		this.blackListGroupe = blackListGroupe;
	}

	public Long getNbLieuxSession() {
		return nbLieuxSession;
	}

	public void setNbLieuxSession(Long nbLieuxSession) {
		this.nbLieuxSession = nbLieuxSession;
	}

	public Long getNbInscritsSession() {
		return nbInscritsSession;
	}

	public void setNbInscritsSession(Long nbInscritsSession) {
		this.nbInscritsSession = nbInscritsSession;
	}

	public Long getNbTagCheckerSession() {
		return nbTagCheckerSession;
	}

	public void setNbTagCheckerSession(Long nbTagCheckerSession) {
		this.nbTagCheckerSession = nbTagCheckerSession;
	}

	public Long getNbPresentsSession() {
		return nbPresentsSession;
	}

	public void setNbPresentsSession(Long nbPresentsSession) {
		this.nbPresentsSession = nbPresentsSession;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNomSessionEpreuve() {
		return nomSessionEpreuve;
	}

	public void setNomSessionEpreuve(String nomSessionEpreuve) {
		this.nomSessionEpreuve = nomSessionEpreuve;
	}

	public Campus getCampus() {
		return campus;
	}

	public void setCampus(Campus campus) {
		this.campus = campus;
	}
	
	public Date getDateExamen() {
		return dateExamen;
	}

	public void setDateExamen(Date dateExamen) {
		this.dateExamen = dateExamen;
	}

	public Date getHeureConvocation() {
		return heureConvocation;
	}

	public void setHeureConvocation(Date heureConvocation) {
		this.heureConvocation = heureConvocation;
	}

	public Date getHeureEpreuve() {
		return heureEpreuve;
	}

	public void setHeureEpreuve(Date heureEpreuve) {
		this.heureEpreuve = heureEpreuve;
	}

	public String getDureeEpreuve() {
		return dureeEpreuve;
	}

	public void setDureeEpreuve(String dureeEpreuve) {
		this.dureeEpreuve = dureeEpreuve;
	}

	public List<MultipartFile> getFiles() {
		return files;
	}

	public void setFiles(List<MultipartFile> files) {
		this.files = files;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Long getNbDispatchTagCheck() {
		return nbDispatchTagCheck;
	}

	public void setNbDispatchTagCheck(Long nbDispatchTagCheck) {
		this.nbDispatchTagCheck = nbDispatchTagCheck;
	}

	public Date getFinEpreuve() {
		return finEpreuve;
	}

	public void setFinEpreuve(Date finEpreuve) {
		this.finEpreuve = finEpreuve;
	}

	public Long getNbCheckedByCardTagCheck() {
		return nbCheckedByCardTagCheck;
	}

	public void setNbCheckedByCardTagCheck(Long nbCheckedByCardTagCheck) {
		this.nbCheckedByCardTagCheck = nbCheckedByCardTagCheck;
	}

	public String getAnneeUniv() {
		return anneeUniv;
	}

	public void setAnneeUniv(String anneeUniv) {
		this.anneeUniv = anneeUniv;
	}

	public Boolean getIsProcurationEnabled() {
		return isProcurationEnabled;
	}

	public void setIsProcurationEnabled(Boolean isProcurationEnabled) {
		this.isProcurationEnabled = isProcurationEnabled;
	}

	public Boolean getIsSessionLibre() {
		return isSessionLibre;
	}

	public void setIsSessionLibre(Boolean isSessionLibre) {
		this.isSessionLibre = isSessionLibre;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Long getNbUnknown() {
		return nbUnknown;
	}

	public void setNbUnknown(Long nbUnknown) {
		this.nbUnknown = nbUnknown;
	}

	public Date getDateArchivage() {
		return dateArchivage;
	}

	public void setDateArchivage(Date dateArchivage) {
		this.dateArchivage = dateArchivage;
	}

	public String getLoginArchivage() {
		return loginArchivage;
	}

	public void setLoginArchivage(String loginArchivage) {
		this.loginArchivage = loginArchivage;
	}

	public TypeSession getTypeSession() {
		return typeSession;
	}

	public void setTypeSession(TypeSession typeSession) {
		this.typeSession = typeSession;
	}

	public Long getNbStoredFiles() {
		return nbStoredFiles;
	}

	public void setNbStoredFiles(Long nbStoredFiles) {
		this.nbStoredFiles = nbStoredFiles;
	}

	public TypeBadgeage getTypeBadgeage() {
		return typeBadgeage;
	}

	public void setTypeBadgeage(TypeBadgeage typeBadgeage) {
		this.typeBadgeage = typeBadgeage;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}

	public int getMaxBadgeageAlert() {
		return maxBadgeageAlert;
	}

	public void setMaxBadgeageAlert(Integer maxBadgeageAlert) {
		this.maxBadgeageAlert = maxBadgeageAlert;
	}

	public Boolean getIsSaveInExcluded() {
		return isSaveInExcluded;
	}

	public void setIsSaveInExcluded(Boolean isSaveInExcluded) {
		this.isSaveInExcluded = isSaveInExcluded;
	}

	public Boolean getIsGroupeDisplayed() {
		return isGroupeDisplayed;
	}

	public void setIsGroupeDisplayed(Boolean isGroupeDisplayed) {
		this.isGroupeDisplayed = isGroupeDisplayed;
	}

	public Statut getStatut() {
		return statut;
	}

	public void setStatut(Statut statut) {
		this.statut = statut;
	}

	public Long getAdeEventId() {
		return adeEventId;
	}

	public void setAdeEventId(Long adeEventId) {
		this.adeEventId = adeEventId;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public Date getDateImport() {
		return dateImport;
	}

	public void setDateImport(Date dateImport) {
		this.dateImport = dateImport;
	}

	public Long getAdeProjectId() {
		return adeProjectId;
	}

	public void setAdeProjectId(Long adeProjectId) {
		this.adeProjectId = adeProjectId;
	}

	public String getNbHours() {
		return nbHours;
	}

	public void setNbHours(String nbHours) {
		this.nbHours = nbHours;
	}

	public Boolean getIsSecondTag() {
		return isSecondTag;
	}

	public void setIsSecondTag(Boolean isSecondTag) {
		this.isSecondTag = isSecondTag;
	}
}
