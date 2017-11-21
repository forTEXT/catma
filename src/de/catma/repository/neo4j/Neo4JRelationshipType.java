package de.catma.repository.neo4j;

// unfortunately can't make this an enum that implements org.neo4j.graphdb.RelationshipType because attribute values
// must be constants (OGM @Relationship attribute)
public final class Neo4JRelationshipType {
	public static final String HAS_PROJECT = "HAS_PROJECT";
	public static final String HAS_WORKTREE = "HAS_WORKTREE";
	public static final String HAS_TAGSET = "HAS_TAGSET";
	public static final String HAS_MARKUP_COLLECTION = "HAS_MARKUP_COLLECTION";
	public static final String HAS_SOURCE_DOCUMENT = "HAS_SOURCE_DOCUMENT";
	public static final String HAS_TAG_DEFINITION = "HAS_TAG_DEFINITION";
	public static final String HAS_SYSTEM_PROPERTY_DEFINITION = "HAS_SYSTEM_PROPERTY_DEFINITION";
	public static final String HAS_USER_PROPERTY_DEFINITION = "HAS_USER_PROPERTY_DEFINITION";
	public static final String HAS_CHILD = "HAS_CHILD";
	public static final String HAS_TAG_INSTANCE = "HAS_TAG_INSTANCE";
	public static final String HAS_SYSTEM_PROPERTY = "HAS_SYSTEM_PROPERTY";
	public static final String HAS_USER_DEFINED_PROPERTY = "HAS_USER_DEFINED_PROPERTY";
	public static final String HAS_RANGE = "HAS_RANGE";
	public static final String HAS_PROPERTY_DEFINITION = "HAS_PROPERTY_DEFINITION";
	public static final String REFERENCES_SOURCE_DOCUMENT = "REFERENCES_SOURCE_DOCUMENT";
	public static final String REFERENCES_TAGSET = "REFERENCES_TAGSET";
	public static final String HAS_TERM = "HAS_TERM";
	public static final String HAS_POSITION = "HAS_POSITION";
	public static final String APPEARS_AFTER = "APPEARS_AFTER";
	public static final String APPEARS_BEFORE = "APPEARS_BEFORE";
}
