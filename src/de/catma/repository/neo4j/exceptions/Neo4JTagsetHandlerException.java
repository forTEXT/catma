package de.catma.repository.neo4j.exceptions;

public class Neo4JTagsetHandlerException extends Exception {
	public Neo4JTagsetHandlerException(String message) {
		super(message);
	}

	public Neo4JTagsetHandlerException(String message, Throwable cause) {
		super(message, cause);
	}
}
