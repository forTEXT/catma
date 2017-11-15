package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.neo4j.exceptions.Neo4JUserMarkupCollectionException;
import de.catma.repository.neo4j.models.Neo4JMarkupCollectionWorktree;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="UserMarkupCollection")
public class Neo4JUserMarkupCollection {
	@Id
	private String uuid;

	private String name;

	@Relationship(type="HAS_WORKTREE", direction=Relationship.OUTGOING)
	private List<Neo4JMarkupCollectionWorktree> markupCollectionWorktrees;

	public Neo4JUserMarkupCollection() {
		this.markupCollectionWorktrees = new ArrayList<>();
	}

	public Neo4JUserMarkupCollection(String uuid, String name) {
		this();

		this.uuid = uuid;
		this.name = name;
	}

	public UserMarkupCollection getMarkupCollectionWorktree(String revisionHash)
			throws Neo4JUserMarkupCollectionException {

		for (Neo4JMarkupCollectionWorktree markupCollectionWorktree : this.markupCollectionWorktrees) {
			if (markupCollectionWorktree.getUserMarkupCollection().getRevisionHash().equals(revisionHash)) {
				return markupCollectionWorktree.getUserMarkupCollection();
			}
		}
		return null;
	}

	public void addMarkupCollectionWorktree(UserMarkupCollection userMarkupCollection)
			throws Neo4JUserMarkupCollectionException {

		// TODO: validation, eg: is there already a worktree for the UserMarkupCollection's revision hash
		this.markupCollectionWorktrees.add(new Neo4JMarkupCollectionWorktree(userMarkupCollection));
	}
}
