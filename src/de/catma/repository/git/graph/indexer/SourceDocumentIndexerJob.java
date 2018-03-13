package de.catma.repository.git.graph.indexer;

import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.isAdjacentTo;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Values;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;

public class SourceDocumentIndexerJob implements Job {
	
	public static enum DataField {
		title,
		projectId,
		rootRevisionHash,
		sourceDocumentId,
		sourceDocumentRevisionHash,
		tokenizedSourceDocumentPath, 
		
	}
	
	private Logger logger = Logger.getLogger(SourceDocumentIndexerJob.class.getName());

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			StatementExcutor.execute(new SessionRunner() {
				@Override
				public void run(Session session) throws Exception {
					JobDataMap data = context.getJobDetail().getJobDataMap();
					String title = (String) data.get(DataField.title.name());
					String projectId = (String)data.get(DataField.projectId.name());
					String rootRevisionHash = (String)data.get(DataField.rootRevisionHash.name());
					String sourceDocumentId = (String)data.get(DataField.sourceDocumentId.name());
					String sourceDocumentRevisionHash = (String)data.get(DataField.sourceDocumentRevisionHash.name());
					String tokenizedSourceDocumentPath = (String)data.get(DataField.tokenizedSourceDocumentPath.name());
					
					logger.info(
						String.format("Start indexing Source Document %s with ID %s for Project ID %s", 
								title, sourceDocumentId, projectId));
					
					int batchSize = 20000;

					session.run(
						"CALL apoc.periodic.iterate(\""
						+ "MATCH (:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
							+ "(s:"+nt(SourceDocument)+"{"
								+ "sourceDocumentId:{pSourceDocumentId}, "
								+ "revisionHash:{pSourceDocumentRevisionHash} "
							+ "}) "
						+ " WITH {pFileUrl} AS url, s "
						+ " CALL apoc.load.json(url) YIELD value"
						+ " UNWIND keys(value) as term"
						+ " RETURN term, value[term] AS positions, s"
						+ "\","
						+ "\""
						+ " CREATE (t:"+nt(Term)+"{literal:term})"
						+ " MERGE (t)-[:"+rt(isPartOf)+"]->(s)"
						+ " WITH t, positions"
						+ " UNWIND positions as position"
						+ " CREATE (p:"+nt(Position)+"{tokenOffset:position.tokenOffset, startOffset:position.startOffset, endOffset:position.endOffset})"
						+ " MERGE (t)-[:"+rt(hasPosition)+"]->(p)"
						+ "\","
						+ "{batchSize:{pBatchSize}, iterateList:{pIterateList}, parallel:{pParallel}, "
						+ " params: {pProjectId:{pProjectId}, pRootRevisionHash:{pRootRevisionHash},"
						+ " pSourceDocumentId:{pSourceDocumentId}, pSourceDocumentRevisionHash:{pSourceDocumentRevisionHash},"
						+ " pFileUrl:{pFileUrl}}})",
						Values.parameters(
							"pBatchSize", batchSize,
							"pIterateList", false,
							"pParallel", true,
							"pProjectId", projectId,
							"pRootRevisionHash", rootRevisionHash,
							"pSourceDocumentId", sourceDocumentId,
							"pSourceDocumentRevisionHash", sourceDocumentRevisionHash,
							"pFileUrl", new URL("file", null, FilenameUtils.separatorsToUnix(tokenizedSourceDocumentPath)).toString()
						)
					);
					logger.info(String.format(
						"Finished loading types and tokens for Source Document %s with ID %s for Project ID %s", 
						title, sourceDocumentId, projectId));
						
					session.run(
						"CALL apoc.periodic.iterate(\""
						+ " MATCH (:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
						+ " (s:"+nt(SourceDocument)+"{"
							+ "sourceDocumentId:{pSourceDocumentId}, "
							+ "revisionHash:{pSourceDocumentRevisionHash} "
						+ " }) "
						+ " <-[:"+rt(isPartOf)+"]-(:"+nt(Term)+")-[:"+rt(hasPosition)+"]->(p:"+nt(Position)+") "
						+ " WITH p order by p.tokenOffset "
						+ " WITH apoc.coll.pairsMin(COLLECT(p)) as posPairs "
						+ " UNWIND posPairs as posPair "
						+ " RETURN posPair"
						+ "\","
						+ "\""
						+ " WITH head(posPair) as p1, last(posPair) as p2 "
						+ " MERGE (p1)-[:"+rt(isAdjacentTo)+"]->(p2)"
						+ "\","
						+ "{batchSize:{pBatchSize}, iterateList:{pIterateList}, parallel:{pParallel}, "
						+ " params: {pProjectId:{pProjectId}, pRootRevisionHash:{pRootRevisionHash},"
						+ " pSourceDocumentId:{pSourceDocumentId}, pSourceDocumentRevisionHash:{pSourceDocumentRevisionHash}}})",
						Values.parameters(
							"pBatchSize", batchSize,
							"pIterateList", false,
							"pParallel", true,
							"pProjectId", projectId,
							"pRootRevisionHash", rootRevisionHash,
							"pSourceDocumentId", sourceDocumentId,
							"pSourceDocumentRevisionHash", sourceDocumentRevisionHash
						)
					);
					
					logger.info(String.format(
							"Finished indexing tokens for Source Document %s with ID %s for Project ID %s", 
							title, sourceDocumentId, projectId));
	
					session.run(
						" MATCH (:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
						+ " (s:"+nt(SourceDocument)+"{"
							+ "sourceDocumentId:{pSourceDocumentId}, "
							+ "revisionHash:{pSourceDocumentRevisionHash} "
						+ " }) "
						+ " SET s.indexCompleted = {pIndexCompleted}",
						Values.parameters(
							"pProjectId", projectId,
							"pRootRevisionHash", rootRevisionHash,
							"pSourceDocumentId", sourceDocumentId,
							"pSourceDocumentRevisionHash", sourceDocumentRevisionHash,
							"pIndexCompleted", true
						)
					);
	
					final IndexBufferManager indexBufferManager = 
							IndexBufferManagerName.INDEXBUFFERMANAGER.getIndeBufferManager();
					indexBufferManager.remove(sourceDocumentId);
					
					logger.info(String.format(
							"Finished indexing Source Document %s with ID %s for Project ID %s", 
							title, sourceDocumentId, projectId));
				}
				
			});
		}
		catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

}
