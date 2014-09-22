package de.catma.indexer.graph;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

public class SourceDocumentIndexer {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public void index(SourceDocument sourceDocument) throws IOException {

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
		
		Map<String, List<TermInfo>> terms = termExtractor.getTerms();

		logger.info("term extraction finished");

		GraphDatabaseService graphDb = null;
		
		try {
			graphDb = (GraphDatabaseService) new InitialContext().lookup(CatmaGraphDbName.CATMAGRAPHDB.name());
		} catch (NamingException e) {
			throw new IOException(e);
		}
		
		logger.info("starting insertion into graph");
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
				
				for (TermInfo ti : termInfos) {
					
					Node positionNode = graphDb.createNode(NodeType.Position);
					orderedTermInfos.add(new NodeTermInfo(ti.getTokenOffset(), positionNode));
					nodeCount++;
					positionNode.setProperty(PositionProperty.position.name(), ti.getTokenOffset());
					positionNode.setProperty(PositionProperty.start.name(), ti.getRange().getStartPoint());
					positionNode.setProperty(PositionProperty.end.name(), ti.getRange().getEndPoint());
					positionNode.setProperty(PositionProperty.literal.name(), ti.getTerm());

					termNode.createRelationshipTo(positionNode, NodeRelationType.HAS_POSITION);
					relCount++;
					
					termNode.createRelationshipTo(sdNode, NodeRelationType.IS_PART_OF);
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
		
		logger.info("insertion of source document finished nodecount " + nodeCount + " relcount " + relCount);
	}

	public void removeSourceDocument(String sourceDocumentID) {
		// TODO Auto-generated method stub
		
	}
	
}


