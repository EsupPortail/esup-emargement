package org.esupportail.emargement.domain;

import javax.naming.Name;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.DnAttribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

/**
 * @author jptran
 *
 */
@Entry(base = "ou=people", objectClasses  = {"inetOrgPerson" })
public final class UserLdap {
    @Id
    private Name id;
     
    private @Attribute(name = "sn") String username;
    private @Attribute(name = "mail") String email;
    private @Attribute(name = "eduPersonPrincipalName") String eppn;
    private @Attribute(name = "cn") String nomPrenom;
    private @Attribute(name = "displayName") String prenomNom;
    private @Attribute(name = "givenName") String prenom;
    private @Attribute(name = "supannEtuId") String numEtudiant;
    private @Attribute(name = "supannCivilite") String civilite;
    private @Attribute(name = "uid") String uid;
    
	public Name getId() {
		return id;
	}

	public void setId(Name id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getEppn() {
		return eppn;
	}
	public void setEppn(String eppn) {
		this.eppn = eppn;
	}
	public String getNomPrenom() {
		return nomPrenom;
	}
	public void setNomPrenom(String nomPrenom) {
		this.nomPrenom = nomPrenom;
	}
	public String getPrenomNom() {
		return prenomNom;
	}
	public void setPrenomNom(String prenomNom) {
		this.prenomNom = prenomNom;
	}
	public String getPrenom() {
		return prenom;
	}
	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}
	public String getNumEtudiant() {
		return numEtudiant;
	}
	public void setNumEtudiant(String numEtudiant) {
		this.numEtudiant = numEtudiant;
	}
	public String getCivilite() {
		return civilite;
	}
	public void setCivilite(String civilite) {
		this.civilite = civilite;
	}
}