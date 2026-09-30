package org.esupportail.emargement.beans;

public class DashboardSession {

    private Long id;

    private String horaire;
    private String nom;
    private String type;
    private String contexte;
    private String salle;
    private String surveillant;
    private String supervisorUrl;
    private boolean enCours;
    private boolean terminee;
    private boolean isLinkDisplayed;

    private int absents;
    private int presents;
    private int participants;
	private int inconnus;

    public DashboardSession() {
    }

    public DashboardSession(
            Long id,
            String horaire,
            String nom,
            String type,
            String contexte,
            String salle,
            String surveillant,
            int presents,
            int participants,
            int absents,
            int inconnus) {

        this.id = id;
        this.horaire = horaire;
        this.nom = nom;
        this.type = type;
        this.contexte = contexte;
        this.salle = salle;
        this.surveillant = surveillant;
        this.presents = presents;
        this.absents = absents;
        this.inconnus = inconnus;
        this.participants = participants;
    }

    public Long getId() {
        return id;
    }

    public String getHoraire() {
        return horaire;
    }

    public String getNom() {
        return nom;
    }

    public String getType() {
        return type;
    }

    public String getContexte() {
        return contexte;
    }

    public String getSalle() {
        return salle;
    }

    public String getSurveillant() {
        return surveillant;
    }

    public int getPresents() {
        return presents;
    }

    public int getParticipants() {
        return participants;
    }
    
    public boolean isEnCours() {
        return enCours;
    }

    public void setEnCours(boolean enCours) {
        this.enCours = enCours;
    }

	public int getAbsents() {
		return absents;
	}

	public void setAbsents(int absents) {
		this.absents = absents;
	}

	public int getInconnus() {
		return inconnus;
	}

	public void setInconnus(int inconnus) {
		this.inconnus = inconnus;
	}

	public String getSupervisorUrl() {
		return supervisorUrl;
	}

	public void setSupervisorUrl(String supervisorUrl) {
		this.supervisorUrl = supervisorUrl;
	}

	public boolean isLinkDisplayed() {
		return isLinkDisplayed;
	}

	public void setLinkDisplayed(boolean isLinkDisplayed) {
		this.isLinkDisplayed = isLinkDisplayed;
	}

	public boolean isTerminee() {
		return terminee;
	}

	public void setTerminee(boolean terminee) {
		this.terminee = terminee;
	}
}