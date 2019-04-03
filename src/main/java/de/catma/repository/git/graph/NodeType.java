package de.catma.repository.git.graph;

public enum NodeType {
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
