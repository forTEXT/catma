package de.catma.repository.neo.serialization.models;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity
public class Neo4JTagDefinition {
	@Id
	private String uuid;
	private String parentUuid;
	private String name;

	//TODO: turn into relationship as well? - already exists from the Tagset...
	private String tagsetDefinitionUuid;

	@Relationship(type = "SYSTEM_PROPERTY_DEFINITION", direction = Relationship.OUTGOING)
	private List<PropertyDefinition> systemPropertyDefinitions;

	@Relationship(type = "USER_PROPERTY_DEFINITION", direction = Relationship.OUTGOING)
	private List<PropertyDefinition> userDefinedPropertyDefinitions;

	@Relationship(type = "PARENT_OF", direction = Relationship.OUTGOING)
	private List<Neo4JTagDefinition> children;

	public Neo4JTagDefinition(){
		this.systemPropertyDefinitions = new ArrayList<>();
		this.userDefinedPropertyDefinitions = new ArrayList<>();

		this.children = new ArrayList<>();
	}

	public Neo4JTagDefinition(TagDefinition tagDefinition, List<TagDefinition> children){
		this();

		this.setTagDefinition(tagDefinition);
		this.setChildren(children);
	}

	public TagDefinition getTagDefinition(){
		TagDefinition tagDefinition = new TagDefinition();

		tagDefinition.setUuid(this.uuid);
		tagDefinition.setParentUuid(this.parentUuid);
		tagDefinition.setName(this.name);
		tagDefinition.setTagsetDefinitionUuid(this.tagsetDefinitionUuid);

		for(PropertyDefinition systemPropertyDefinition : this.systemPropertyDefinitions){
			tagDefinition.addSystemPropertyDefinition(systemPropertyDefinition);
		}

		for(PropertyDefinition userPropertyDefinition : this.userDefinedPropertyDefinitions){
			tagDefinition.addUserDefinedPropertyDefinition(userPropertyDefinition);
		}

		return tagDefinition;
	}

	public void setTagDefinition(TagDefinition tagDefinition){
		this.uuid = tagDefinition.getUuid();
		this.parentUuid = tagDefinition.getParentUuid();
		this.name = tagDefinition.getName();
		this.tagsetDefinitionUuid = tagDefinition.getTagsetDefinitionUuid();

		this.systemPropertyDefinitions.clear();
		this.systemPropertyDefinitions.addAll(tagDefinition.getSystemPropertyDefinitions());

		this.userDefinedPropertyDefinitions.clear();
		this.userDefinedPropertyDefinitions.addAll(tagDefinition.getUserDefinedPropertyDefinitions());

		this.setChildren(null);
	}

	public void setChildren(List<TagDefinition> children){
		this.children.clear();

		if(children == null){
			return;
		}

		for(TagDefinition tagDefinition : children){
			this.children.add(new Neo4JTagDefinition(tagDefinition, null));
		}
	}
}
