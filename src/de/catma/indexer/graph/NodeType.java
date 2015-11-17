package de.catma.indexer.graph;

import org.neo4j.graphdb.Label;

public enum NodeType implements Label {
	SourceDocument,
	Position,
	Term,
	;
}