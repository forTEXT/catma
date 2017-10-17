package de.catma.repository.git.exceptions;


public class TagsetHandlerException extends Exception {
	public TagsetHandlerException(String message) {
		super(message);
	}

	public TagsetHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
