package de.catma.indexer.graph;

import org.neo4j.graphdb.RelationshipType;

public enum NodeRelationType implements RelationshipType {
	IS_PART_OF,
	ADJACENT_TO,
	HAS_POSITION,
	;
}