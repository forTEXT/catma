package de.catma.repository.git.graph;

public enum RelationType{
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
	hasProperty,
	hasInstance,
	;
	
	public static String rt(RelationType relationType) {
		return relationType.name();
	}
}
