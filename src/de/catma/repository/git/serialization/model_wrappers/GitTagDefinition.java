package de.catma.repository.git.serialization.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.tag.TagDefinition;

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

	//TODO: property definitions

	public String getParentUuid(){return this.tagDefinition.getParentUuid();}

	public void setParentUuid(String uuid){this.tagDefinition.setParentUuid(uuid);}



}
