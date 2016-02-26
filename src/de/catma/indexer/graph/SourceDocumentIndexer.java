package de.catma.indexer.graph;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.indexer.indexbuffer.IndexBufferManager;

public class SourceDocumentIndexer {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public void index(final SourceDocument sourceDocument, BackgroundService backgroundService) throws IOException {

		logger.info("start indexing sourcedocument");
		
		List<String> unseparableCharacterSequences = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getUnseparableCharacterSequences();
		List<Character> userDefinedSeparatingCharacters = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getUserDefinedSeparatingCharacters();
		Locale locale = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				.getIndexInfoSet().getLocale();
		
		TermExtractor termExtractor = 
				new TermExtractor(
					sourceDocument.getContent(), 
					unseparableCharacterSequences, 
					userDefinedSeparatingCharacters, 
					locale);
		
		final Map<String, List<TermInfo>> terms = termExtractor.getTerms();

		logger.info("term extraction finished");
		
		final IndexBufferManager indexBufferManager = 
				IndexBufferManagerName.INDEXBUFFERMANAGER.getIndeBufferManager();

		
		indexBufferManager.add(sourceDocument.getID(), terms);
		
		logger.info("buffering finished");
		
		backgroundService.submit(
			new DefaultProgressCallable<String>() {
				@Override
				public String call() throws Exception {
					return insertIntoGraph(sourceDocument, terms, getProgressListener());
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

	protected String insertIntoGraph(
			SourceDocument sourceDocument, Map<String, List<TermInfo>> terms, 
			ProgressListener progressListener) throws IOException {
		GraphDatabaseService graphDb = 
					CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService();
		
		progressListener.setProgress("starting insertion into graph " + sourceDocument);
		long nodeCount = 0;
		long relCount = 0;
		
		long commitAfterNodeCount = RepositoryPropertyKey.commitAfterNodeCount.getValue(20000);
		long commitAfterRelationCount= RepositoryPropertyKey.commitAfterRelationCount.getValue(40000);
		
		
		Transaction tx = null;
		
        try 
        {
        	tx = graphDb.beginTx();
        	
			Node sdNode = graphDb.createNode(NodeType.SourceDocument);
			nodeCount++;
			sdNode.setProperty(SourceDocumentProperty.localUri.name(), sourceDocument.getID());
			sdNode.setProperty(SourceDocumentProperty.title.name(), sourceDocument.toString());
			
			TreeSet<NodeTermInfo> orderedTermInfos = new TreeSet<NodeTermInfo>();
			
			for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
				String term = entry.getKey();
				List<TermInfo> termInfos = entry.getValue();
				
				Node termNode = graphDb.createNode(NodeType.Term);
				nodeCount++;
				termNode.setProperty(TermProperty.literal.name(), term);
				termNode.setProperty(TermProperty.freq.name(), termInfos.size());
				
				termNode.createRelationshipTo(sdNode, NodeRelationType.IS_PART_OF);
				relCount++;
				for (TermInfo ti : termInfos) {
					
					Node positionNode = graphDb.createNode(NodeType.Position);
					orderedTermInfos.add(new NodeTermInfo(ti.getTokenOffset(), positionNode));
					nodeCount++;
					positionNode.setProperty(PositionProperty.position.name(), ti.getTokenOffset());
					positionNode.setProperty(PositionProperty.start.name(), ti.getRange().getStartPoint());
					positionNode.setProperty(PositionProperty.end.name(), ti.getRange().getEndPoint());
					
					termNode.createRelationshipTo(positionNode, NodeRelationType.HAS_POSITION);
					relCount++;
					
					if (nodeCount % commitAfterNodeCount == 0) {
						progressListener.setProgress(
								"graph insertion: commiting transaction nodecount "
										+ nodeCount + " relcount " + relCount + " " + sourceDocument);
						tx.success();
						tx.close();
						tx = graphDb.beginTx();
					}
				}
				
			}
			
			NodeTermInfo prevTi = null;
			for (NodeTermInfo ti : orderedTermInfos) {
	
				if (prevTi != null) {
					prevTi.getNode().createRelationshipTo(
						ti.getNode(), 
						NodeRelationType.ADJACENT_TO);
					relCount++;
				}
				prevTi = ti;
				
				if (relCount % commitAfterRelationCount == 0) {
					progressListener.setProgress(
							"graph insertion: commiting transaction nodecount "
									+ nodeCount + " relcount " + relCount + " " + sourceDocument);
					tx.success();
					tx.close();
					tx = graphDb.beginTx();
				}
			}
	
			tx.success();
        }
        finally {
        	if (tx != null) {
        		tx.close();
        	}
        }
		
        progressListener.setProgress(
        	"insertion of source document finished nodecount "
        			+ nodeCount + " relcount " + relCount + " " + sourceDocument);
		return sourceDocument.getID();
	}

	public void removeSourceDocument(String sourceDocumentID) throws IOException {

		GraphDatabaseService graphDb = 
					CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService();
		
		try (Transaction tx = graphDb.beginTx()) {

			ResourceIterator<Node> sdNodeIterator = graphDb.findNodes(
					NodeType.SourceDocument, 
					SourceDocumentProperty.localUri.name(), 
					sourceDocumentID);
			if (sdNodeIterator.hasNext()) {
				Node sdNode = sdNodeIterator.next();
				sdNodeIterator.close();
				sdNode.setProperty(SourceDocumentProperty.deleted.name(), Boolean.TRUE);
				tx.success();
			}
			else {
				sdNodeIterator.close();
			}
		}		

	}
	
}


