package de.catma.repository.git.exceptions;

public class GitTagsetHandlerException extends Exception {
	public GitTagsetHandlerException(String message) {
		super(message);
	}

	public GitTagsetHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
