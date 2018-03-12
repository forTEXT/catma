package de.catma.repository.git.graph;

import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.rt;

import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Values;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.project.ProjectReference;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;
import de.catma.repository.neo4j.ValueContainer;

public class GraphProjectHandler {

	private Logger logger = Logger.getLogger(GraphProjectHandler.class.getName());
	
	public void ensureProjectRevisionIsLoaded(ProjectReference projectReference, String revisionHash) throws Exception {
		ValueContainer<Boolean> revisionExists = new ValueContainer<>();
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					"MATCH (p:"+nt(Project)+"{projectId:{pProjectId}})"
					+ " -[:"+rt(hasRevision)+"]-> "
					+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pRevisionHash}}) "
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
			
			StatementExcutor.execute(new SessionRunner() {
				@Override
				public void run(Session session) throws Exception {

					session.run(
						"MERGE (p:"+nt(Project)+"{projectId:{pProjectId}})"
						+ " MERGE (p) "
						+ " -[:"+rt(hasRevision)+"]-> "
						+ "(pr:"+nt(ProjectRevision)+"{revisionHash:{pRevisionHash}}) ", 
						Values.parameters(
							"pProjectId", projectReference.getProjectId(),
							"pRevisionHash", revisionHash
						)
					);

				}
			});
			
			
		}
		
	}

	public void insertSourceDocument(
			String projectId, String oldRootRevisionHash, String rootRevisionHash, SourceDocument sourceDocument, 
			Path tokenizedSourceDocumentPath, IndexBufferManager indexBufferManager, BackgroundService backgroundService) {
		backgroundService.submit(
			new DefaultProgressCallable<String>() {
				@Override
				public String call() throws Exception {
					return insertIntoGraph(projectId, oldRootRevisionHash, rootRevisionHash, sourceDocument, tokenizedSourceDocumentPath, getProgressListener());
				}
			},
			new ExecutionListener<String>() {
				@Override
				public void done(String result) {
					indexBufferManager.remove(result);
				}
				@Override
				public void error(Throwable t) {
					logger.log(Level.SEVERE, "error indexing document #" + sourceDocument.getID(), t);
				}
			},
			new LogProgressListener());		
	}

	private String insertIntoGraph(String projectId, String oldRootRevisionHash, String rootRevisionHash, SourceDocument sourceDocument,
			Path tokenizedSourceDocumentPath, ProgressListener progressListener) throws Exception {

//		"file:///catmagit/mp/CATMA_82011484-6250-4694-AE3F-E1B76BFFE5D8_test/CATMA_469F02CB-8196-4078-B069-DBBB19A6C7FC_sourcedocument/CATMA_469F02CB-8196-4078-B069-DBBB19A6C7FC.json"
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				SourceDocumentInfo info = sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
				
				progressListener.setProgress("Start indexing Source Document");
				
				try (Transaction tx = session.beginTransaction()) {
					tx.run(
						" MATCH (:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->(pr:"+nt(ProjectRevision)+"{revisionHash:{pOldRootRevisionHash}}) "
						+ " SET pr.revisionHash = {pRootRevisionHash}"
						+ " WITH pr "
						+ " MERGE (pr)-[:"+rt(hasDocument)+"]->"
								+ "(s:SourceDocument{"
									+ "sourceDocumentId:{pSourceDocumentId}, "
									+ "revisionHash:{pSourceDocumentRevisionHash}, "
									+ "locale:{pLocale}, "
									+ "publisher:{pPublisher}, "
									+ "author:{pAuthor}, "
									+ "description:{pDescription}"
								+ "}) "
						+ " WITH {pFileUrl} AS url, s "
						+ " CALL apoc.load.json(url) YIELD value"
						+ " UNWIND keys(value) as term"
						+ " CREATE (t:Term{literal:term})"
						+ " MERGE (t)-[:isPartOf]->(s)"
						+ " WITH value, term, t, s"
						+ " UNWIND value[term] as position"
						+ " CREATE (p:Position{tokenOffset:position.tokenOffset, startOffset:position.startOffset, endOffset:position.endOffset})"
						+ " MERGE (t)-[:hasPosition]->(p)",
						Values.parameters(
							"pProjectId", projectId,
							"pOldRootRevisionHash", oldRootRevisionHash,
							"pRootRevisionHash", rootRevisionHash,
							"pSourceDocumentId", sourceDocument.getID(),
							"pSourceDocumentRevisionHash", sourceDocument.getRevisionHash(),
							"pLocale", info.getIndexInfoSet().getLocale().toString(),
							"pPublisher", info.getContentInfoSet().getPublisher(),
							"pAuthor", info.getContentInfoSet().getAuthor(),
							"pDescription", info.getContentInfoSet().getDescription(),
							"pFileUrl", new URL("file", null, FilenameUtils.separatorsToUnix(tokenizedSourceDocumentPath.toString())).toString()
						));
					logger.info("Finished loading types and tokens");
					
					tx.run(
						"MATCH (s:SourceDocument{"
							+ "sourceDocumentId:{pSourceDocumentId}, "
							+ "revisionHash:{pSourceDocumentRevisionHash}"
						+ "})"
						+ "<-[:isPartOf]-(:Term)-[:hasPosition]->(p:Position) "
						+ " WITH p order by p.tokenOffset "
						+ " WITH apoc.coll.pairsMin(COLLECT(p)) as posPairs "
						+ " UNWIND posPairs as posPair "
						+ " WITH head(posPair) as p1, last(posPair) as p2 "
						+ " MERGE (p1)-[:isAdjacentTo]->(p2) ",
						Values.parameters(
							"pSourceDocumentId", sourceDocument.getID(),
							"pSourceDocumentRevisionHash", sourceDocument.getRevisionHash()
						)
					);
					
					tx.success();
				}
				
				progressListener.setProgress("Finished indexing Source Document");
			}
		});
		
		return sourceDocument.getID();
	}

}
