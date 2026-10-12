package org.esupportail.emargement.beans;

import java.time.LocalDate;

public class ParticipantStart {
	private final String view;
	private final LocalDate date;

	public ParticipantStart(String view, LocalDate date) {
		this.view = view;
		this.date = date;
	}

	public String getView() {
		return view;
	}

	public LocalDate getDate() {
		return date;
	}

}