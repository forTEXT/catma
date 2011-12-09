package de.catma.core.tag;

import java.util.ArrayList;
import java.util.List;

public class TagDefinition implements Versionable {
	
	// TODO: hier besser interface einziehen um Links als tagdefs abzubilden.
	
	private String id;
	private String type;
	private Version version;
	private List<PropertyDefinition> systemPropertyDefinitions;
	private List<PropertyDefinition> userDefinedPropertyDefinitions;
	private String baseID;


	public TagDefinition(String id, String type, Version version, String baseID) {
		super();
		this.id = id;
		this.type = type;
		this.version = version;
		this.baseID = baseID;
		systemPropertyDefinitions = new ArrayList<PropertyDefinition>();
		userDefinedPropertyDefinitions = new ArrayList<PropertyDefinition>();
	}

	public Version getVersion() {
		return version;
	}
	
	
	@Override
	public String toString() {
		return "TAG_DEF[" + type + ",#" + id +","+version+((baseID==null) ? "]" : (",#"+baseID+"]"));
	}

	public void addSystemPropertyDefinition(PropertyDefinition propertyDefinition) {
		systemPropertyDefinitions.add(propertyDefinition);
	}
	
	public void addUserDefinedPropertyDefinition(PropertyDefinition propertyDefinition) {
		userDefinedPropertyDefinitions.add(propertyDefinition);
	}	
	
}
