package org.esupportail.emargement.beans;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardData {

    private int sessions;
    private int participants;
    private int presents;
    private int absents;
    private int encours;

    private List<DashboardSession> todaySessions;
    private List<DashboardWeekDay> month;
    private List<DashboardWeekDay> week;
    private LocalDate weekStart;
    private LocalDate monthStart;
    private LocalDate yearStart;
    
    private boolean hasSupervisorSessions;
    
    private List<DashboardWeekDay> year = new ArrayList<>();
    
    private String anneeUniversitaire;
    
    public int getSessions() {
        return sessions;
    }

    public void setSessions(int sessions) {
        this.sessions = sessions;
    }

    public int getParticipants() {
        return participants;
    }

    public void setParticipants(int participants) {
        this.participants = participants;
    }

    public int getPresents() {
        return presents;
    }

    public void setPresents(int presents) {
        this.presents = presents;
    }

    public int getAbsents() {
        return absents;
    }

    public void setAbsents(int absents) {
        this.absents = absents;
    }

    public List<DashboardSession> getTodaySessions() {
        return todaySessions;
    }

    public void setTodaySessions(List<DashboardSession> todaySessions) {
        this.todaySessions = todaySessions;
    }

    public List<DashboardWeekDay> getWeek() {
		return week;
	}

	public void setWeek(List<DashboardWeekDay> week) {
		this.week = week;
	}

	public int getEncours() {
		return encours;
	}

	public void setEncours(int encours) {
		this.encours = encours;
	}

	public LocalDate getWeekStart() {
		return weekStart;
	}

	public void setWeekStart(LocalDate weekStart) {
		this.weekStart = weekStart;
	}

	public List<DashboardWeekDay> getMonth() {
		return month;
	}

	public void setMonth(List<DashboardWeekDay> month) {
		this.month = month;
	}

	public LocalDate getMonthStart() {
		return monthStart;
	}

	public void setMonthStart(LocalDate monthStart) {
		this.monthStart = monthStart;
	}

	public LocalDate getYearStart() {
		return yearStart;
	}

	public void setYearStart(LocalDate yearStart) {
		this.yearStart = yearStart;
	}

	public List<DashboardWeekDay> getYear() {
		return year;
	}

	public void setYear(List<DashboardWeekDay> year) {
		this.year = year;
	}

	public String getAnneeUniversitaire() {
		return anneeUniversitaire;
	}

	public void setAnneeUniversitaire(String anneeUniversitaire) {
		this.anneeUniversitaire = anneeUniversitaire;
	}

	public boolean isHasSupervisorSessions() {
		return hasSupervisorSessions;
	}

	public void setHasSupervisorSessions(boolean hasSupervisorSessions) {
		this.hasSupervisorSessions = hasSupervisorSessions;
	}
	
}