package de.catma.core.document.standoffmarkup.usermarkup;

public class UserMarkupCollectionReference {
	
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
}
