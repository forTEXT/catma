package de.catma.repository.neo4j.model_wrappers;

import de.catma.repository.neo4j.Neo4JRelationshipType;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="TagDefinition")
public class Neo4JTagDefinition {
	@Id
	@GeneratedValue
	private Long id;

	private String uuid;
	private String parentUuid;
	private String name;

	//TODO: turn into relationship as well? - already exists from the Tagset...
	private String tagsetDefinitionUuid;

	@Relationship(type=Neo4JRelationshipType.HAS_SYSTEM_PROPERTY_DEFINITION, direction=Relationship.OUTGOING)
	private List<Neo4JPropertyDefinition> systemPropertyDefinitions;

	@Relationship(type=Neo4JRelationshipType.HAS_USER_PROPERTY_DEFINITION, direction = Relationship.OUTGOING)
	private List<Neo4JPropertyDefinition> userDefinedPropertyDefinitions;

	@Relationship(type=Neo4JRelationshipType.HAS_CHILD, direction = Relationship.OUTGOING)
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

	public Long getId() {
		return this.id;
	}

	public String getUuid() {
		return this.uuid;
	}

	public String getParentUuid() {
		return this.parentUuid;
	}

	public String getName() {
		return this.name;
	}

	public String getTagsetDefinitionUuid() {
		return this.tagsetDefinitionUuid;
	}

	public TagDefinition getTagDefinition(){
		TagDefinition tagDefinition = new TagDefinition();

		tagDefinition.setUuid(this.uuid);
		tagDefinition.setParentUuid(this.parentUuid);
		tagDefinition.setName(this.name);
		tagDefinition.setTagsetDefinitionUuid(this.tagsetDefinitionUuid);

		for(Neo4JPropertyDefinition systemPropertyDefinition : this.systemPropertyDefinitions){
			tagDefinition.addSystemPropertyDefinition(systemPropertyDefinition.getPropertyDefinition());
		}

		for(Neo4JPropertyDefinition userPropertyDefinition : this.userDefinedPropertyDefinitions){
			tagDefinition.addUserDefinedPropertyDefinition(userPropertyDefinition.getPropertyDefinition());
		}

		return tagDefinition;
	}

	public void setTagDefinition(TagDefinition tagDefinition){
		this.uuid = tagDefinition.getUuid();
		this.parentUuid = tagDefinition.getParentUuid();
		this.name = tagDefinition.getName();
		this.tagsetDefinitionUuid = tagDefinition.getTagsetDefinitionUuid();

		this.systemPropertyDefinitions.clear();
		for(PropertyDefinition systemPropertyDefinition : tagDefinition.getSystemPropertyDefinitions()){
			this.systemPropertyDefinitions.add(new Neo4JPropertyDefinition(systemPropertyDefinition));
		}

		this.userDefinedPropertyDefinitions.clear();
		for(PropertyDefinition userPropertyDefinition : tagDefinition.getUserDefinedPropertyDefinitions()){
			this.userDefinedPropertyDefinitions.add(new Neo4JPropertyDefinition(userPropertyDefinition));
		}

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
