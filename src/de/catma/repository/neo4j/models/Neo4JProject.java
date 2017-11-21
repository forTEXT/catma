package de.catma.repository.neo4j.models;

import de.catma.models.Project;
import de.catma.repository.neo4j.exceptions.Neo4JProjectException;
import de.catma.repository.neo4j.model_wrappers.Neo4JProjectWorktree;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="Project")
public class Neo4JProject {
	@Id
	@GeneratedValue
	private Long id;

	private String uuid;
	private String name;

	@Relationship(type="HAS_WORKTREE", direction=Relationship.OUTGOING)
	private List<Neo4JProjectWorktree> projectWorktrees;

	public Neo4JProject() {
		this.projectWorktrees = new ArrayList<>();
	}

	public Neo4JProject(String uuid, String name) {
		this();

		this.uuid = uuid;
		this.name = name;
	}

	public String getUuid() {
		return this.uuid;
	}

	public String getName() {
		return this.name;
	}

	public List<Neo4JProjectWorktree> getNeo4JProjectWorktrees() {
		return this.projectWorktrees;
	}

	public Neo4JProjectWorktree getNeo4JProjectWorktree(String revisionHash) {
		// https://stackoverflow.com/questions/22694884/filter-java-stream-to-1-and-only-1-element
		// https://blog.codefx.org/java/stream-findfirst-findany-reduce/
		return this.projectWorktrees.stream().filter(
				neo4JProjectWorktree -> neo4JProjectWorktree.getRevisionHash().equals(revisionHash)
		).reduce(
				(a, b) -> { throw new IllegalStateException("Multiple elements where there should be only one"); }
		).orElse(null);
	}

	public Project getProjectRevision(String revisionHash) throws Neo4JProjectException {
		Neo4JProjectWorktree neo4JProjectWorktree = this.getNeo4JProjectWorktree(revisionHash);
		if (neo4JProjectWorktree != null) {
			return neo4JProjectWorktree.getProject();
		}
		return null;
	}

	public void setProjectRevision(Project project) throws Neo4JProjectException {
		Neo4JProjectWorktree neo4JProjectWorktree = this.getNeo4JProjectWorktree(project.getRevisionHash());

		if (neo4JProjectWorktree == null) {
			neo4JProjectWorktree = new Neo4JProjectWorktree(project);
			this.projectWorktrees.add(neo4JProjectWorktree);
		}
		else {
			neo4JProjectWorktree.setProject(project);
		}
	}
}
