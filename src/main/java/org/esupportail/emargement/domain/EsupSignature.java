package org.esupportail.emargement.domain;

import java.util.Date;

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
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class EsupSignature {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
		
	@ManyToOne
	@JsonIgnore
	private Context context;
	
    public static enum TypeSignature {
        SESSION, INDIVIDUAL
    };
    
    public static enum StatutSignature {
        PENDING, COMPLETED, DOWNLOADED, DELETED, ENDED, EXPORTED
    };

    @Column
    @Enumerated(EnumType.STRING)
    public TypeSignature TypeSignature;
    
    @Column
    @Enumerated(EnumType.STRING)
    public StatutSignature statutSignature;
     
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date dateModification;
	
    @ManyToOne
    private SessionEpreuve sessionEpreuve;
    
	@Column	
	public Long storedFileId;
	
	@Column	
	public Long signRequestId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDateModification() {
		return dateModification;
	}

	public void setDateModification(Date dateModification) {
		this.dateModification = dateModification;
	}

	public SessionEpreuve getSessionEpreuve() {
		return sessionEpreuve;
	}

	public void setSessionEpreuve(SessionEpreuve sessionEpreuve) {
		this.sessionEpreuve = sessionEpreuve;
	}
	
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Long getSignRequestId() {
		return signRequestId;
	}

	public void setSignRequestId(Long signRequestId) {
		this.signRequestId = signRequestId;
	}

	public Long getStoredFileId() {
		return storedFileId;
	}

	public void setStoredFileId(Long storedFileId) {
		this.storedFileId = storedFileId;
	}

	public TypeSignature getTypeSignature() {
		return TypeSignature;
	}

	public void setTypeSignature(TypeSignature typeSignature) {
		TypeSignature = typeSignature;
	}

	public StatutSignature getStatutSignature() {
		return statutSignature;
	}

	public void setStatutSignature(StatutSignature statutSignature) {
		this.statutSignature = statutSignature;
	}

}
