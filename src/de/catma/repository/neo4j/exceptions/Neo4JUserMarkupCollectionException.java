package de.catma.repository.neo4j.exceptions;

public class Neo4JUserMarkupCollectionException extends Exception {
	public Neo4JUserMarkupCollectionException(String message) {
		super(message);
	}

	public Neo4JUserMarkupCollectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
