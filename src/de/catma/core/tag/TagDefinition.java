package de.catma.core.tag;

import java.util.HashMap;
import java.util.Map;

public class TagDefinition implements Versionable {
	
	// TODO: hier besser interface einziehen um Links als tagdefs abzubilden.
	
	private String id;
	private String type;
	private Version version;
	private Map<String,PropertyDefinition> systemPropertyDefinitions;
	private Map<String,PropertyDefinition> userDefinedPropertyDefinitions;
	private String baseID;


	public TagDefinition(String id, String type, Version version, String baseID) {
		super();
		this.id = id;
		this.type = type;
		this.version = version;
		this.baseID = baseID;
		systemPropertyDefinitions = new HashMap<String, PropertyDefinition>();
		userDefinedPropertyDefinitions = new HashMap<String, PropertyDefinition>();
	}

	public Version getVersion() {
		return version;
	}
	
	
	@Override
	public String toString() {
		return "TAG_DEF[" + type + ",#" + id +","+version+((baseID==null) ? "]" : (",#"+baseID+"]"));
	}

	public void addSystemPropertyDefinition(PropertyDefinition propertyDefinition) {
		systemPropertyDefinitions.put(propertyDefinition.getName(), propertyDefinition);
	}
	
	public void addUserDefinedPropertyDefinition(PropertyDefinition propertyDefinition) {
		userDefinedPropertyDefinitions.put(propertyDefinition.getName(), propertyDefinition);
	}	
	
	public String getID() {
		return id;
	}

	public PropertyDefinition getPropertyDefinition(String propertyName) {
		if (systemPropertyDefinitions.containsKey(propertyName)) {
			return systemPropertyDefinitions.get(propertyName);
		}
		else {
			return userDefinedPropertyDefinitions.get(propertyName);
		}
	}
}
