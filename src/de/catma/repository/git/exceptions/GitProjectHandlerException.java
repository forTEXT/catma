package de.catma.repository.git.exceptions;

public class GitProjectHandlerException extends Exception {
	public GitProjectHandlerException(String message) {
		super(message);
	}

	public GitProjectHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
