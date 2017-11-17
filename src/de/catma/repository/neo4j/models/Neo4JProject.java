package de.catma.repository.neo4j.models;

import de.catma.models.Project;
import de.catma.repository.neo4j.exceptions.Neo4JProjectException;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="Project")
public class Neo4JProject {
	@Id
	private String uuid;

	private String name;

	@Relationship(type="HAS_WORKTREE", direction= Relationship.OUTGOING)
	private List<Neo4JProjectWorktree> projectWorktrees;

	public Neo4JProject() {
		this.projectWorktrees = new ArrayList<>();
	}

	public Neo4JProject(String uuid, String name) {
		this();

		this.uuid = uuid;
		this.name = name;
	}

	public Project getProjectWorktree(String revisionHash) throws Neo4JProjectException {

		for (Neo4JProjectWorktree projectWorktree : this.projectWorktrees) {
			if (projectWorktree.getProject().getRevisionHash().equals(revisionHash)) {
				return projectWorktree.getProject();
			}
		}
		return null;
	}

	public void addProjectWorktree(Project project) throws Neo4JProjectException {
		// TODO: validation, eg: is there already a worktree for the Project's revision hash
		this.projectWorktrees.add(new Neo4JProjectWorktree(project));
	}
}
