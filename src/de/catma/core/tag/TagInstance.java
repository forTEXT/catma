package de.catma.core.tag;

import java.util.HashMap;
import java.util.Map;

public class TagInstance {

	private String ID;
	private TagDefinition tagDefinition;
	private Map<String,Property> systemProperties; // TODO: store user as system properties
	private Map<String,Property> userDefinedProperties;
	
	public TagInstance(String ID, TagDefinition tagDefinition) {
		this.ID = ID;
		this.tagDefinition = tagDefinition;
		systemProperties = new HashMap<String, Property>();
		userDefinedProperties = new HashMap<String, Property>();
	}
	
	public TagDefinition getTagDefinition() {
		return tagDefinition;
	}

	public void addSystemProperty(Property property) {
		systemProperties.put(property.getName(), property);
	}
	
	public void addUserDefinedProperty(Property property) {
		userDefinedProperties.put(property.getName(), property);
	}
	
	@Override
	public String toString() {
		return "TAGINSTANCE[#"+ID+","+tagDefinition+"]";
	}
}
