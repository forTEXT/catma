package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.repository.neo4j.models.Neo4JTagsetWorktree;
import de.catma.tag.TagsetDefinition;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="TagsetDefinition")
public class Neo4JTagsetDefinition {
	@Id
	private String uuid;
	private String name;

	@Relationship(type="HAS_WORKTREE", direction=Relationship.OUTGOING)
	private List<Neo4JTagsetWorktree> tagsetWorktrees;

	public Neo4JTagsetDefinition() {
		this.tagsetWorktrees = new ArrayList<>();
	}

	public Neo4JTagsetDefinition(String uuid, String name) {
		this();

		this.uuid = uuid;
		this.name = name;
	}

	public TagsetDefinition getTagsetWorktree(String revisionHash) {
		for (Neo4JTagsetWorktree tagsetWorktree : this.tagsetWorktrees) {
			if (tagsetWorktree.getTagsetDefinition().getRevisionHash().equals(revisionHash)) {
				return tagsetWorktree.getTagsetDefinition();
			}
		}
		return null;
	}

	public void addTagsetWorktree(TagsetDefinition tagsetDefinition) {
		// TODO: validation, eg: is there already a worktree for the TagsetDefinition's revision hash
		this.tagsetWorktrees.add(new Neo4JTagsetWorktree(tagsetDefinition));
	}
}
