package de.catma.repository.git.serialization.model_wrappers;

import java.util.TreeMap;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;

public class GitTagDefinition {
	private String name;
	private String parentUuid;
	private TreeMap<String,PropertyDefinition> systemPropertyDefinitions;
	private String tagsetDefinitionUuid;
	private TreeMap<String,PropertyDefinition> userDefinedPropertyDefinitions;
	private String uuid;
	
	public GitTagDefinition() {
		this.systemPropertyDefinitions = new TreeMap<String, PropertyDefinition>();
		this.userDefinedPropertyDefinitions = new TreeMap<String, PropertyDefinition>();
	}

	public GitTagDefinition(TagDefinition tagDefinition) {
		this();
		
		this.name = tagDefinition.getName();
		this.parentUuid = tagDefinition.getParentUuid();
		for(PropertyDefinition propertyDefinition : tagDefinition.getSystemPropertyDefinitions()){
			systemPropertyDefinitions.put(propertyDefinition.getUuid(), propertyDefinition);
		}
		this.tagsetDefinitionUuid = tagDefinition.getTagsetDefinitionUuid();
		for(PropertyDefinition propertyDefinition : tagDefinition.getUserDefinedPropertyDefinitions()){
			userDefinedPropertyDefinitions.put(propertyDefinition.getUuid(), propertyDefinition);
		}
		this.uuid = tagDefinition.getUuid();
		
	}

	public TagDefinition getTagDefinition() {
		TagDefinition tag = new TagDefinition(uuid, name, parentUuid, tagsetDefinitionUuid);
		this.systemPropertyDefinitions.values().forEach(prop -> tag.addSystemPropertyDefinition(prop));
		this.userDefinedPropertyDefinitions.values().forEach(prop -> tag.addUserDefinedPropertyDefinition(prop));
		return tag;
	}
}
