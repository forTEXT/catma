package de.catma.core.tag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TagDefinition implements Versionable {
	
	// TODO: hier besser interface einziehen um Links als tagdefs abzubilden.
	
	private static enum SystemPropertyName {
		catma_displaycolor,
		;
	}
	
	public final static TagDefinition CATMA_BASE_TAG = 
			new TagDefinition(
				"CATMA_BASE_TAG", "CATMA_BASE_TAG", new Version("1"), null);
	
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
		if ((this.baseID != null) && this.baseID.isEmpty()) {
			this.baseID = null;
		}
		systemPropertyDefinitions = new HashMap<String, PropertyDefinition>();
		userDefinedPropertyDefinitions = new HashMap<String, PropertyDefinition>();
	}

	public Version getVersion() {
		return version;
	}
	
	
	@Override
	public String toString() {
		return "TAG_DEF[" + type 
				+ ",#" + id +","
				+version
				+((baseID==null) ? "]" : (",#"+baseID+"]"));
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
	
	public Collection<PropertyDefinition> getUserDefinedPropertyDefinitions() {
		return Collections.unmodifiableCollection(userDefinedPropertyDefinitions.values());
	}
	
	public String getBaseID() {
		return baseID;
	}
	
	public String getType() {
		return type;
	}
	
	public String getColor() {
		return systemPropertyDefinitions.get(
				SystemPropertyName.catma_displaycolor.name()).getFirstValue();
	}

	public Collection<PropertyDefinition> getSystemPropertyDefinitions() {
		return Collections.unmodifiableCollection(
				systemPropertyDefinitions.values());
	}
}
