package de.catma.repository.git.graph;

import org.neo4j.graphdb.Label;

public enum NodeType implements Label {
	User,
	Project, 
	ProjectRevision,
	
	SourceDocument,
	Term,
	Position,
	
	MarkupCollection,
	TagInstance,
	AnnotationProperty,
	
	Tagset,
	Tag,
	Property,
	
	DeletedProject,
	DeletedTag,
	DeletedProperty,
	;
	
	public static String nt(NodeType nodeType) {
		return nodeType.name();
	}
}
