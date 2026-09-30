package org.esupportail.emargement.beans;

import java.time.LocalDate;

public class DashboardMonthDay {

    private LocalDate date;
    private int sessions;
    private int participants;
    private int presents;
    private int absents;
    private int unknown;
    private int nonEmarges;

    public boolean isToday() {
        return LocalDate.now().equals(date);
    }

    public boolean isFuture() {
        return date.isAfter(LocalDate.now());
    }

    public int getTauxPresence() {
        if (participants == 0) {
            return 0;
        }

        return (int) Math.round(
                presents * 100.0 / participants);
    }

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
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

	public int getUnknown() {
		return unknown;
	}

	public void setUnknown(int unknown) {
		this.unknown = unknown;
	}

	public int getNonEmarges() {
		return nonEmarges;
	}

	public void setNonEmarges(int nonEmarges) {
		this.nonEmarges = nonEmarges;
	}

}