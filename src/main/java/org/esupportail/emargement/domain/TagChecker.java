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

import org.esupportail.emargement.domain.TagCheck.TypeEmargement;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class TagChecker implements ContextSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@ManyToOne
	private Context context;
	
    @ManyToOne
    private SessionLocation sessionLocation;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date tagDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date tagDate2;
    
    @ManyToOne
    private UserApp tagValidator;
    
    @ManyToOne
    private UserApp tagValidator2;

    @ManyToOne
    private UserApp userApp;
    
    @Column
    @Enumerated(EnumType.STRING)
    private TypeEmargement typeEmargement;
    
    @Column
    @Enumerated(EnumType.STRING)
    private TypeEmargement typeEmargement2;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SessionLocation getSessionLocation() {
		return sessionLocation;
	}

	public void setSessionLocation(SessionLocation sessionLocation) {
		this.sessionLocation = sessionLocation;
	}

	public UserApp getUserApp() {
		return userApp;
	}

	public void setUserApp(UserApp userApp) {
		this.userApp = userApp;
	}

	public SessionEpreuve getSessionEpreuve() {
		return sessionLocation.getSessionEpreuve();
	}
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Date getTagDate() {
		return tagDate;
	}

	public void setTagDate(Date tagDate) {
		this.tagDate = tagDate;
	}

	public Date getTagDate2() {
		return tagDate2;
	}

	public void setTagDate2(Date tagDate2) {
		this.tagDate2 = tagDate2;
	}

	public TypeEmargement getTypeEmargement() {
		return typeEmargement;
	}

	public void setTypeEmargement(TypeEmargement typeEmargement) {
		this.typeEmargement = typeEmargement;
	}

	public UserApp getTagValidator() {
		return tagValidator;
	}

	public void setTagValidator(UserApp tagValidator) {
		this.tagValidator = tagValidator;
	}

	public TypeEmargement getTypeEmargement2() {
		return typeEmargement2;
	}

	public void setTypeEmargement2(TypeEmargement typeEmargement2) {
		this.typeEmargement2 = typeEmargement2;
	}

	public UserApp getTagValidator2() {
		return tagValidator2;
	}

	public void setTagValidator2(UserApp tagValidator2) {
		this.tagValidator2 = tagValidator2;
	}
}
