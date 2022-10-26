package de.catma.repository.git.managers;

public class CommitMissingException extends Exception {

	public CommitMissingException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitMissingException(String message) {
		super(message);
	}
}
