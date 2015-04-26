package de.catma.indexer.graph;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressListener;
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
		
		try {
			final IndexBufferManager indexBufferManager = 
					(IndexBufferManager) new InitialContext().lookup(
							IndexBufferManagerName.INDEXBUFFERMANAGER.name());

			
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
		} catch (NamingException e) {
			throw new IOException(e);
		}
	}

	protected String insertIntoGraph(
			SourceDocument sourceDocument, Map<String, List<TermInfo>> terms, 
			ProgressListener progressListener) throws IOException {
		GraphDatabaseService graphDb = null;
		
		try {
			graphDb = 
				(GraphDatabaseService) new InitialContext().lookup(
					CatmaGraphDbName.CATMAGRAPHDB.name());
		} catch (NamingException e) {
			throw new IOException(e);
		}
		
		progressListener.setProgress("starting insertion into graph");
		long nodeCount = 0;
		long relCount = 0;
		
        try ( Transaction tx = graphDb.beginTx() )
        {
			
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
			}
	
			tx.success();
        }
		
        progressListener.setProgress(
        	"insertion of source document finished nodecount "
        			+ nodeCount + " relcount " + relCount);
		return sourceDocument.getID();
	}

	public void removeSourceDocument(String sourceDocumentID) throws IOException {

		GraphDatabaseService graphDb = null;
		
		try {
			graphDb = 
				(GraphDatabaseService) new InitialContext().lookup(
					CatmaGraphDbName.CATMAGRAPHDB.name());
		} catch (NamingException e) {
			throw new IOException(e);
		}
		
		try (Transaction tx = graphDb.beginTx()) {
			
			ResourceIterable<Node> sdNodeIterable = 
					graphDb.findNodesByLabelAndProperty(
							NodeType.SourceDocument, 
							SourceDocumentProperty.localUri.name(), 
							sourceDocumentID);
			ResourceIterator<Node> sdNodeIterator = sdNodeIterable.iterator();
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


