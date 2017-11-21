package de.catma.repository.git.exceptions;

public class GitSourceDocumentHandlerException extends Exception {
	public GitSourceDocumentHandlerException(String message) {
		super(message);
	}

	public GitSourceDocumentHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
