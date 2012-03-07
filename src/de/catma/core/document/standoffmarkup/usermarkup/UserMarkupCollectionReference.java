package de.catma.core.document.standoffmarkup.usermarkup;

import de.catma.core.document.standoffmarkup.MarkupCollectionReference;

public class UserMarkupCollectionReference implements MarkupCollectionReference {
	
	private String id;
	private String name;
	
	public UserMarkupCollectionReference(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
}
