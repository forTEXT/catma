package de.catma.ui.client.ui.tag;

import java.util.HashMap;
import java.util.Map;

public class CTagInstance {

	private String id;
	private CTagDefinition tagDefinition;
	private Map<String,CProperty> systemProperties;
	private Map<String,CProperty> userDefinedProperties;
	
	public CTagInstance(String id, CTagDefinition tagDefinition) {
		this.id = id;
		this.tagDefinition = tagDefinition;
		systemProperties = new HashMap<String, CProperty>();
		userDefinedProperties = new HashMap<String, CProperty>();
	}
	
	public CTagDefinition getTagDefinition() {
		return tagDefinition;
	}

	public void addSystemProperty(CProperty property) {
		systemProperties.put(property.getName(), property);
	}
	
	public void addUserDefinedProperty(CProperty property) {
		userDefinedProperties.put(property.getName(), property);
	}
	
	@Override
	public String toString() {
		return "TAGINSTANCE[#"+id+","+tagDefinition+"]";
	}
}
