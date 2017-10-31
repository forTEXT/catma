package de.catma.repository.neo.exceptions;

public class NeoTagsetHandlerException extends Exception {
	public NeoTagsetHandlerException(String message) {
		super(message);
	}

	public NeoTagsetHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
