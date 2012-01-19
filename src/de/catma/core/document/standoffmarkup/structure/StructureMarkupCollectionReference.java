package de.catma.core.document.standoffmarkup.structure;

public class StructureMarkupCollectionReference {
	
	private String id;
	private String name;
	
	public StructureMarkupCollectionReference(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
