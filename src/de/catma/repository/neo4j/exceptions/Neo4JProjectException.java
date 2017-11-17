package de.catma.repository.neo4j.exceptions;

public class Neo4JProjectException extends Exception {
	public Neo4JProjectException(String message) {
		super(message);
	}

	public Neo4JProjectException(String message, Throwable cause) {
		super(message, cause);
	}
}
