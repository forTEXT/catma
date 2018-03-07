package de.catma.repository.git.graph;

import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;

import de.catma.document.source.SourceDocument;
import de.catma.project.ProjectReference;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;
import de.catma.repository.neo4j.ValueContainer;

public class GraphProjectHandler {
	
	public void ensureProjectRevisionIsLoaded(ProjectReference projectReference, String revisionHash) throws Exception {
		ValueContainer<Boolean> revisionExists = new ValueContainer<>();
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (p:"+NodeType.Project.name()+"{projectId:{pProjectId}})"
					+ " -[:"+RelationType.hasRevision.name()+"]-> "
					+ "(pr:"+NodeType.ProjectRevision.name()+"{revisionHash:{pRevisionHash}}) "
							+ " RETURN p.projectId ", 
					Values.parameters(
						"pProjectId", projectReference.getProjectId(),
						"pRevisionHash", revisionHash
					)
				);
				
				revisionExists.setValue(statementResult.hasNext());
			}
		});
		
		if (!revisionExists.getValue()) {
			//TODO: implement load
			
			
			
			
		}
		
	}

	public void insertSourceDocument(String projectId, String rootRevisionHash, SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

}
