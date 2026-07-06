package org.esupportail.emargement.beans;

import java.util.List;

import org.esupportail.emargement.domain.SessionEpreuve;
import org.esupportail.emargement.domain.TagCheck;

public class ExportContext {

	List<TagCheck> list;
	String nomFichier;
	String fin;
	SessionEpreuve se;

	public ExportContext(List<TagCheck> list, String nomFichier, String fin, SessionEpreuve se) {
		this.list = list;
		this.nomFichier = nomFichier;
		this.fin = fin;
		this.se = se;
	}

	public List<TagCheck> getList() {
		return list;
	}

	public void setList(List<TagCheck> list) {
		this.list = list;
	}

	public String getNomFichier() {
		return nomFichier;
	}

	public void setNomFichier(String nomFichier) {
		this.nomFichier = nomFichier;
	}

	public String getFin() {
		return fin;
	}

	public void setFin(String fin) {
		this.fin = fin;
	}

	public SessionEpreuve getSe() {
		return se;
	}

	public void setSe(SessionEpreuve se) {
		this.se = se;
	}
}
