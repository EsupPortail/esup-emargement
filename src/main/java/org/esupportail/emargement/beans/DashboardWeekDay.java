package org.esupportail.emargement.beans;

import java.time.LocalDate;

public class DashboardWeekDay {

    private LocalDate date;
    private String contextKey;

    private int sessions;
    private int participants;
    private int presents;
    private int absents;
    private int nonEmarges;
    private int unknown;
    
    public boolean isToday() {
        return LocalDate.now().equals(date);
    }
    
    public boolean isCurrentMonth() {
        LocalDate now = LocalDate.now();

        return now.getYear() == date.getYear()
                && now.getMonthValue() == date.getMonthValue();
    }

    public int getTauxPresence() {

        if (participants == 0) {
            return 0;
        }

        return (int) Math.round(
                presents * 100.0 / participants);
    }

    public DashboardWeekDay() {
    }

    public DashboardWeekDay(LocalDate date, String contextKey) {
        this.date = date;
        this.contextKey = contextKey;
    }

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getContextKey() {
		return contextKey;
	}

	public void setContextKey(String contextKey) {
		this.contextKey = contextKey;
	}

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

	public int getNonEmarges() {
		return nonEmarges;
	}

	public void setNonEmarges(int nonEmarges) {
		this.nonEmarges = nonEmarges;
	}

	public int getUnknown() {
		return unknown;
	}

	public void setUnknown(int unknown) {
		this.unknown = unknown;
	}
	
}