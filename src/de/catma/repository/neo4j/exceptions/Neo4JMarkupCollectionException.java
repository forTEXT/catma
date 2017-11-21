package de.catma.repository.neo4j.exceptions;

public class Neo4JMarkupCollectionException extends Exception {
	public Neo4JMarkupCollectionException(String message) {
		super(message);
	}

	public Neo4JMarkupCollectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
