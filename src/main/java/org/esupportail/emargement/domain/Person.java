package org.esupportail.emargement.domain;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class Person implements ContextSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@ManyToOne
	private Context context;
	
    private String eppn;

    private String numIdentifiant;
    
    private String type;
    
    @Transient
    private String civilite;
    
    @Transient
    private String nom;

    @Transient
    private String prenom;

	@Column(name = "is_boursier", columnDefinition = "boolean default false")
	private boolean isBoursier = false;

	@Column(name = "is_rse", columnDefinition = "boolean default false")
	private boolean isRSE = false;

	@Column(name = "formation_type")
	private String formationType;

	@Column(name = "has_tier_tempory", columnDefinition = "boolean default false")
	private boolean hasTierTempory = false;

	@Column(name = "lock_sync_ade", columnDefinition = "boolean default true")
	private boolean lockSyncAde = true;
    
	@Column(name = "lock_sync_ldap", columnDefinition = "boolean default true")
	private boolean lockSyncLdap = true;

	@Column(name = "formasup_linked", columnDefinition = "boolean default false")
	private boolean formasupLinked = false;

	@Column(name = "user_app_updated_at", columnDefinition = "timestamp default null")
	private LocalDateTime UserAppUpdatedAt;

	@Column(name = "sync_ade_updated_at", columnDefinition = "timestamp default null")
	private LocalDateTime syncUpdatedAt;
	
	@Column(name = "created_at", columnDefinition = "timestamp default current_timestamp")
	private LocalDateTime createdAt;

	@ManyToOne
	private UserApp userModif;

    @ManyToMany(mappedBy = "persons")
    @JsonIgnoreProperties("persons")
    private Set<Groupe> groupes = new HashSet<>();
    
    public Set<Groupe> getGroupes() {
		return groupes;
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

	public Long getId() {
		return id;
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

	public String getNumIdentifiant() {
		return numIdentifiant;
	}

	public void setNumIdentifiant(String numIdentifiant) {
		this.numIdentifiant = numIdentifiant;
	}
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCivilite() {
		return civilite;
	}

	public void setCivilite(String civilite) {
		this.civilite = civilite;
	}

	public boolean getIsBoursier() {
		return isBoursier;
	}

	public void setIsBoursier(boolean isBoursier) {
		this.isBoursier = isBoursier;
	}

	public boolean getIsRSE() {
		return isRSE;
	}

	public void setIsRSE(boolean isRSE) {
		this.isRSE = isRSE;
	}

	public String getFormationType() {
		return formationType;
	}

	public void setFormationType(String formationType) {
		this.formationType = formationType;
	}

	public boolean getHasTierTempory() {
		return hasTierTempory;
	}

	public void setHasTierTempory(boolean hasTierTempory) {
		this.hasTierTempory = hasTierTempory;
	}

	public boolean getLockSyncAde() {
		return lockSyncAde;
	}

	public void setLockSyncAde(boolean lockSyncAde) {
		this.lockSyncAde = lockSyncAde;
	}

	public boolean getLockSyncLdap() {
		return lockSyncLdap;
	}

	public void setLockSyncLdap(boolean lockSyncLdap) {
		this.lockSyncLdap = lockSyncLdap;
	}

	public boolean getFormasupLinked() {
		return formasupLinked;
	}

	public void setFormasupLinked(boolean formasupLinked) {
		this.formasupLinked = formasupLinked;
	}

	public LocalDateTime getUserAppUpdatedAt() {
		return UserAppUpdatedAt;
	}

	public void setUserAppUpdatedAt(LocalDateTime UserAppUpdatedAt) {
		this.UserAppUpdatedAt = UserAppUpdatedAt;
	}

	public LocalDateTime getSyncUpdatedAt() {
		return syncUpdatedAt;
	}

	public void setSyncUpdatedAt(LocalDateTime syncUpdatedAt) {
		this.syncUpdatedAt = syncUpdatedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public UserApp getUserModif() {
		return userModif;
	}

	public void setUserModif(UserApp userModif) {
		this.userModif = userModif;
	}

}
