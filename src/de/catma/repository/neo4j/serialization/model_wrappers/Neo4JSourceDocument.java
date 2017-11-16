package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.document.source.SourceDocument;
import de.catma.repository.neo4j.exceptions.Neo4JSourceDocumentException;
import de.catma.repository.neo4j.models.Neo4JSourceDocumentWorktree;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="SourceDocument")
public class Neo4JSourceDocument {
	@Id
	private String uuid;

	private String title;

	@Relationship(type="HAS_WORKTREE", direction=Relationship.OUTGOING)
	private List<Neo4JSourceDocumentWorktree> sourceDocumentWorktrees;

	public Neo4JSourceDocument() {
		this.sourceDocumentWorktrees = new ArrayList<>();
	}

	public Neo4JSourceDocument(String uuid, String title) {
		this();

		this.uuid = uuid;
		this.title = title;
	}

	public SourceDocument getSourceDocumentWorktree(String revisionHash) throws Neo4JSourceDocumentException {
		for (Neo4JSourceDocumentWorktree sourceDocumentWorktree : this.sourceDocumentWorktrees) {
			if (sourceDocumentWorktree.getSourceDocument().getRevisionHash().equals(revisionHash)) {
				return sourceDocumentWorktree.getSourceDocument();
			}
		}
		return null;
	}

	public void addSourceDocumentWorktree(SourceDocument sourceDocument) throws Neo4JSourceDocumentException {
		// TODO: validation, eg: is there already a worktree for the SourceDocument's revision hash
		this.sourceDocumentWorktrees.add(new Neo4JSourceDocumentWorktree(sourceDocument));
	}
}
