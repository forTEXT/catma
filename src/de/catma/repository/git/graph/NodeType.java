package de.catma.repository.git.graph;

import org.neo4j.graphdb.Label;

public enum NodeType implements Label {
	Project, 
	ProjectRevision,
	
	;
}
