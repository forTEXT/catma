package de.catma.core.tag;

public class TagLibraryReference {

	private String name;
	private String id;
	
	public TagLibraryReference(String name, String id) {
		super();
		this.name = name;
		this.id = id;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String getId() {
		return id;
	}
}
