package de.catma.repository.git.graph;

import static de.catma.repository.git.graph.NodeType.DeletedProject;
import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.rt;

import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Values;

import de.catma.repository.git.GitUser;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;

public class GraphProjectDeletionHandler {
	
	private GitUser user;
	
	public GraphProjectDeletionHandler(GitUser user) {
		super();
		this.user = user;
	}

	public void deleteProject(String projectId) throws Exception {
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				session.run(
					"MATCH (:"+nt(NodeType.User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+"(p:"+nt(Project)+"{projectId:{pProjectId}}) "
					+"REMOVE p:"+nt(Project)+ " "
					+"SET p:"+nt(DeletedProject)+ " ",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectId
					)
				);
			}
		});
	}
}
