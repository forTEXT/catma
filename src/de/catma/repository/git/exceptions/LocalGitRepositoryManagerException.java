package de.catma.repository.git.exceptions;

public class LocalGitRepositoryManagerException extends Exception {
	public LocalGitRepositoryManagerException(String message) {
		super(message);
	}

	public LocalGitRepositoryManagerException(String message, Throwable cause) {
		super(message, cause);
	}
}
