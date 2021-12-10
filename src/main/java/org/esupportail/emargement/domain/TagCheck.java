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
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class TagCheck implements ContextSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@ManyToOne
	private Context context;
	
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date tagDate;

    @ManyToOne
    private Person person;
    
    @ManyToOne
    private Guest guest;

    private Boolean isTiersTemps = false;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date dateEnvoiConvocation;

    @ManyToOne
    private TagChecker tagChecker;

    @ManyToOne
    private SessionEpreuve sessionEpreuve;

    @ManyToOne
    private SessionLocation sessionLocationBadged;

    @ManyToOne
    private SessionLocation sessionLocationExpected;
    
    public static enum TypeEmargement {
        CARD, LINK, MANUAL, QRCODE
    };
    
    @Column
    @Enumerated(EnumType.STRING)
    private TypeEmargement typeEmargement;
    
    private String numAnonymat;
    
    private Boolean isUnknown = false;
    
    private Boolean isExempt = false;
    
    @ManyToOne
    private Person proxyPerson;
    
    private String codeEtape;
    
    private Boolean checkLdap = true;
    
    private String sessionToken;
    
    @Transient
    private String nomPrenom;
    
    private Integer nbBadgeage;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Date getDateEnvoiConvocation() {
		return dateEnvoiConvocation;
	}

	public void setDateEnvoiConvocation(Date dateEnvoiConvocation) {
		this.dateEnvoiConvocation = dateEnvoiConvocation;
	}

	public Date getTagDate() {
		return tagDate;
	}

	public void setTagDate(Date tagDate) {
		this.tagDate = tagDate;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Boolean getIsTiersTemps() {
		return isTiersTemps;
	}

	public void setIsTiersTemps(Boolean isTiersTemps) {
		this.isTiersTemps = isTiersTemps;
	}

	public TagChecker getTagChecker() {
		return tagChecker;
	}

	public void setTagChecker(TagChecker tagChecker) {
		this.tagChecker = tagChecker;
	}

	public SessionEpreuve getSessionEpreuve() {
		return sessionEpreuve;
	}

	public void setSessionEpreuve(SessionEpreuve sessionEpreuve) {
		this.sessionEpreuve = sessionEpreuve;
	}

	public SessionLocation getSessionLocationBadged() {
		return sessionLocationBadged;
	}

	public void setSessionLocationBadged(SessionLocation sessionLocationBadged) {
		this.sessionLocationBadged = sessionLocationBadged;
	}

	public SessionLocation getSessionLocationExpected() {
		return sessionLocationExpected;
	}

	public void setSessionLocationExpected(SessionLocation sessionLocationExpected) {
		this.sessionLocationExpected = sessionLocationExpected;
	}

	public String getNumAnonymat() {
		return numAnonymat;
	}

	public void setNumAnonymat(String numAnonymat) {
		this.numAnonymat = numAnonymat;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Boolean getIsUnknown() {
		return isUnknown;
	}

	public void setIsUnknown(Boolean isUnknown) {
		this.isUnknown = isUnknown;
	}

	public Person getProxyPerson() {
		return proxyPerson;
	}

	public void setProxyPerson(Person proxyPerson) {
		this.proxyPerson = proxyPerson;
	}

	public String getCodeEtape() {
		return codeEtape;
	}

	public void setCodeEtape(String codeEtape) {
		this.codeEtape = codeEtape;
	}

	public Boolean getCheckLdap() {
		return checkLdap;
	}

	public void setCheckLdap(Boolean checkLdap) {
		this.checkLdap = checkLdap;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	public TypeEmargement getTypeEmargement() {
		return typeEmargement;
	}

	public void setTypeEmargement(TypeEmargement typeEmargement) {
		this.typeEmargement = typeEmargement;
	}

	public Guest getGuest() {
		return guest;
	}

	public void setGuest(Guest guest) {
		this.guest = guest;
	}

	public Boolean getIsExempt() {
		return isExempt;
	}

	public void setIsExempt(Boolean isExempt) {
		this.isExempt = isExempt;
	}

	public String getNomPrenom() {
		return nomPrenom;
	}

	public void setNomPrenom(String nomPrenom) {
		this.nomPrenom = nomPrenom;
	}

	public Integer getNbBadgeage() {
		return nbBadgeage;
	}

	public void setNbBadgeage(Integer nbBadgeage) {
		this.nbBadgeage = nbBadgeage;
	}
}
