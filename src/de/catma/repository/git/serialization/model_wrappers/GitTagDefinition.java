package de.catma.repository.git.serialization.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;

import java.util.HashMap;
import java.util.TreeMap;

public class GitTagDefinition {
	private TagDefinition tagDefinition;

	public GitTagDefinition(){
		this.tagDefinition = new TagDefinition();
	}

	public GitTagDefinition(TagDefinition tagDefinition){
		this.tagDefinition = tagDefinition;
	}

	@JsonIgnore
	public TagDefinition getTagDefinition() {
		return this.tagDefinition;
	}

//	private String uuid;
//	private String name;
//	private Version version;
//	private Map<String,PropertyDefinition> systemPropertyDefinitions;
//	private Map<String,PropertyDefinition> userDefinedPropertyDefinitions;
//	private String parentUuid;

	public String getUuid(){return this.tagDefinition.getUuid();}

	public void setUuid(String uuid){this.tagDefinition.setUuid(uuid);}

	public String getName(){return this.tagDefinition.getName();}

	public void setName(String name){this.tagDefinition.setName(name);}

	public TreeMap<String, GitPropertyDefinition> getSystemPropertyDefinitions(){
		TreeMap<String, GitPropertyDefinition> newMap = new TreeMap<>();

		for(PropertyDefinition propertyDefinition : this.tagDefinition.getSystemPropertyDefinitions()){
			newMap.put(propertyDefinition.getUuid(), new GitPropertyDefinition(propertyDefinition));
		}

		return newMap;
	}

	public void setSystemPropertyDefinitions(HashMap<String, GitPropertyDefinition> systemPropertyDefinitions){
		for (GitPropertyDefinition value: systemPropertyDefinitions.values()) {
			this.tagDefinition.addSystemPropertyDefinition(value.getPropertyDefinition());
		}
	}

	public TreeMap<String, GitPropertyDefinition> getUserDefinedPropertyDefinitions(){
		TreeMap<String, GitPropertyDefinition> newMap = new TreeMap<>();

		for(PropertyDefinition propertyDefinition : this.tagDefinition.getUserDefinedPropertyDefinitions()){
			newMap.put(propertyDefinition.getUuid(), new GitPropertyDefinition(propertyDefinition));
		}

		return newMap;
	}

	public void setUserDefinedPropertyDefinitions(HashMap<String, GitPropertyDefinition> userPropertyDefinitions){
		for (GitPropertyDefinition value: userPropertyDefinitions.values()) {
			this.tagDefinition.addUserDefinedPropertyDefinition(value.getPropertyDefinition());
		}
	}

	public String getParentUuid(){return this.tagDefinition.getParentUuid();}

	public void setParentUuid(String uuid){this.tagDefinition.setParentUuid(uuid);}

	public String getTagsetDefinitionUuid() {
		return this.tagDefinition.getTagsetDefinitionUuid();
	}

	public void setTagsetDefinitionUuid(String tagsetDefinitionUuid) {
		this.tagDefinition.setTagsetDefinitionUuid(tagsetDefinitionUuid);
	}
}
