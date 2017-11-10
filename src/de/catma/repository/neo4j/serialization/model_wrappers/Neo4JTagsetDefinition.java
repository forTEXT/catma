package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.tag.TagsetDefinition;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import de.catma.tag.TagDefinition;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="TagsetDefinition")
public class Neo4JTagsetDefinition {
	@Id
	private String uuid;
	private String name;

	@Relationship(type = "HAS_TAG_DEFINITION", direction = Relationship.OUTGOING)
	private List<Neo4JTagDefinition> tagDefinitions;

	public Neo4JTagsetDefinition(){
		this.tagDefinitions = new ArrayList<>();
	}

	public Neo4JTagsetDefinition(TagsetDefinition tagsetDefinition){
		this();

		this.setTagsetDefinition(tagsetDefinition);
	}

	public TagsetDefinition getTagsetDefinition(){
		TagsetDefinition tagsetDefinition = new TagsetDefinition();
		tagsetDefinition.setUuid(this.uuid);
		tagsetDefinition.setName(this.name);

		for(Neo4JTagDefinition neo4JTagDefinition : this.tagDefinitions){
			tagsetDefinition.addTagDefinition(neo4JTagDefinition.getTagDefinition());
		}

		return tagsetDefinition;
	}

	public void setTagsetDefinition(TagsetDefinition tagsetDefinition){
		this.uuid = tagsetDefinition.getUuid();
		this.name = tagsetDefinition.getName();

		this.tagDefinitions.clear();
		for(TagDefinition tagDefinition : tagsetDefinition){
			this.tagDefinitions.add(new Neo4JTagDefinition(tagDefinition, tagsetDefinition.getDirectChildren(tagDefinition)));
		}
	}
}