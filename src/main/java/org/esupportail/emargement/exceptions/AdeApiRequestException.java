package org.esupportail.emargement.exceptions;

public class AdeApiRequestException extends Exception {

	public AdeApiRequestException(String message) {
		super(message);
	}

	public AdeApiRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public AdeApiRequestException(Throwable cause) {
		super(cause);
	}
}
