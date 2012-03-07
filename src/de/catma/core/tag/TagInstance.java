package de.catma.core.tag;

import java.util.HashMap;
import java.util.Map;

public class TagInstance {

	private String id;
	private TagDefinition tagDefinition;
	private Map<String,Property> systemProperties;
	private Map<String,Property> userDefinedProperties;
	
	public TagInstance(String id, TagDefinition tagDefinition) {
		this.id = id;
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
		return "TAGINSTANCE[#"+id+","+tagDefinition+"]";
	}
	
	public String getID() {
		return id;
	}
	
	public Property getSystemProperty(String name) {
		return systemProperties.get(name);
	}
}
