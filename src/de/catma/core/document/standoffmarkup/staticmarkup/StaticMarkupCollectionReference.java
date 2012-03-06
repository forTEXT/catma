package de.catma.core.document.standoffmarkup.staticmarkup;

import de.catma.core.document.standoffmarkup.MarkupCollectionReference;

public class StaticMarkupCollectionReference implements MarkupCollectionReference {
	
	private String id;
	private String name;
	
	public StaticMarkupCollectionReference(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
