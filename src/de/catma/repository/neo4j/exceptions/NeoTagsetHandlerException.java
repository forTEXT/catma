package de.catma.repository.neo4j.exceptions;

public class NeoTagsetHandlerException extends Exception {
	public NeoTagsetHandlerException(String message) {
		super(message);
	}

	public NeoTagsetHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
