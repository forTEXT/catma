package de.catma.repository.git.exceptions;

public class RemoteGitServerManagerException extends Exception {
	public RemoteGitServerManagerException(String message) {
		super(message);
	}

	public RemoteGitServerManagerException(String message, Throwable cause) {
		super(message, cause);
	}
}
