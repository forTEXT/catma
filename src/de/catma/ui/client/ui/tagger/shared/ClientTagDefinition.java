package de.catma.ui.client.ui.tagger.shared;

public class ClientTagDefinition {
	
	public static enum SerializationField {
		tagDefinitionID,
		colorHexValue,
		;
	}
	
	private String id;
	private String color;
	
	public ClientTagDefinition(String id, String color) {
		this.id = id;
		this.color = color;
	}
	
	public String getId() {
		return id;
	}
	
	public String getColor() {
		return color;
	}
	
}
