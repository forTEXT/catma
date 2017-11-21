package de.catma.repository.neo4j.model_wrappers;

import de.catma.models.Project;
import de.catma.repository.neo4j.exceptions.Neo4JProjectException;
import de.catma.repository.neo4j.models.Neo4JProject;
import de.catma.user.User;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity(label="User")
public class Neo4JUser {
	@Id
	private String identifier;

	@Relationship(type="HAS_PROJECT", direction=Relationship.OUTGOING)
	private List<Neo4JProject> projects;

	public Neo4JUser() {
		this.projects = new ArrayList<>();
	}

	public Neo4JUser(User user) {
		this();

		this.identifier = user.getIdentifier();
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public List<Neo4JProject> getNeo4JProjects() {
		return this.projects;
	}

	public Neo4JProject getNeo4JProject(String uuid) {
		// https://stackoverflow.com/questions/22694884/filter-java-stream-to-1-and-only-1-element
		// https://blog.codefx.org/java/stream-findfirst-findany-reduce/
		return this.projects.stream().filter(
				neo4JProject -> neo4JProject.getUuid().equals(uuid)
		).reduce(
				(a, b) -> { throw new IllegalStateException("Multiple elements where there should be only one"); }
		).orElse(null);
	}

	public Project getProject(String uuid, String revisionHash) throws Neo4JProjectException {
		Neo4JProject neo4JProject = this.getNeo4JProject(uuid);
		if (neo4JProject != null && neo4JProject.getNeo4JProjectWorktree(revisionHash) != null) {
			return neo4JProject.getProjectRevision(revisionHash);
		}
		return null;
	}

	public void setProject(Project project) throws Neo4JProjectException {
		Neo4JProject neo4JProject = this.getNeo4JProject(project.getUuid());

		if (neo4JProject == null) {
			neo4JProject = new Neo4JProject(project.getUuid(), project.getName());
			this.projects.add(neo4JProject);
		}

		neo4JProject.setProjectRevision(project);
	}
}
