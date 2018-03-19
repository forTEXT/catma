package de.catma.repository.git.graph;

import org.neo4j.graphdb.RelationshipType;

public enum RelationType implements RelationshipType {
	hasProject,
	hasRevision,
	hasDocument,
	isPartOf,
	isAdjacentTo,
	hasPosition,
	hasCollection,
	hasTagset,
	hasTag, 
	hasParent,
	;
	
	public static String rt(RelationType relationType) {
		return relationType.name();
	}
}
