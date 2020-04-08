package org.esupportail.emargement.domain;

import java.util.Date;

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
public class UserApp {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
	@ManyToOne
	private Context context;
	
    private String eppn;
    
    @Transient
    private String civilite;
    
    @Transient
    private String nom;
    
    @Transient
    private String prenom;
    
    private int contextPriority = 0;
    
	public static enum Role {
		ADMIN, MANAGER, SUPERVISOR
	}
    
	@Enumerated(EnumType.STRING)
    private Role userRole;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date lastConnexion;
    
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date dateCreation;

    public Long getId() {
		return id;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEppn() {
		return eppn;
	}

	public void setEppn(String eppn) {
		this.eppn = eppn;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public Role getUserRole() {
		return userRole;
	}

	public void setUserRole(Role usserRole) {
		this.userRole = usserRole;
	}

	public Date getLastConnexion() {
		return lastConnexion;
	}

	public void setLastConnexion(Date lastConnexion) {
		this.lastConnexion = lastConnexion;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public int getContextPriority() {
		return contextPriority;
	}

	public void setContextPriority(int contextPriority) {
		this.contextPriority = contextPriority;
	}

	public String getCivilite() {
		return civilite;
	}

	public void setCivilite(String civilite) {
		this.civilite = civilite;
	}

}
