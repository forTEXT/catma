package de.catma.repository.neo4j.exceptions;

public class Neo4JSourceDocumentException extends Exception {
	public Neo4JSourceDocumentException(String message) {
		super(message);
	}

	public Neo4JSourceDocumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
