package de.catma.indexer.graph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

public class SourceDocumentIndexer {
	
	public enum RelType implements RelationshipType {
		IS_PART_OF,
		ADJACENT_TO,
		HAS_POSITION,
		;
	}
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
		logger.info("starting graphdb");
		
		GraphDatabaseService graphDb = null;
		
		try {
			graphDb = (GraphDatabaseService) new InitialContext().lookup(CatmaGraphDbName.CATMAGRAPHDB.name());
		} catch (NamingException e) {
			throw new IOException(e);
		}
		
		logger.info("graph db started");
		
		Label sourceDocLabel = DynamicLabel.label( "SourceDocument" );
		
		Transaction tx = graphDb.beginTx();
		
		Node sdNode = graphDb.createNode(sourceDocLabel);
		
		sdNode.setProperty("localUri", sourceDocument.getID());
		sdNode.setProperty("title", sourceDocument.toString());
		
		Label termLabel = DynamicLabel.label("Term");
		Label positionLabel = DynamicLabel.label("Position");
		
		Map<TermInfo,Long> termInfoToNodeId = new HashMap<TermInfo, Long>();
		TreeSet<TermInfo> orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
		
		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			String term = entry.getKey();
			List<TermInfo> termInfos = entry.getValue();
			
			Node termNode = graphDb.createNode(termLabel);
			termNode.setProperty("literal", term);
			
			for (TermInfo ti : termInfos) {
				orderedTermInfos.add(ti);
				
				Node positionNode = graphDb.createNode(positionLabel);
				positionNode.setProperty("position", ti.getTokenOffset());
				positionNode.setProperty("start", ti.getRange().getStartPoint());
				positionNode.setProperty("end", ti.getRange().getEndPoint());
				positionNode.setProperty("literal", ti.getTerm());
				
				termInfoToNodeId.put(ti, positionNode.getId());
				Relationship rsHasPosition = termNode.createRelationshipTo(positionNode, RelType.HAS_POSITION);
				rsHasPosition.setProperty("sourceDoc", sourceDocument.getID());
				
				termNode.createRelationshipTo(sdNode, RelType.IS_PART_OF);
			}
		}
		
		TermInfo prevTi = null;
		for (TermInfo ti : orderedTermInfos) {

			if (prevTi != null) {
				graphDb.getNodeById(termInfoToNodeId.get(prevTi)).createRelationshipTo(
					graphDb.getNodeById(termInfoToNodeId.get(ti)), 
					RelType.ADJACENT_TO);
			}
			prevTi = ti;
		}

		tx.success();
		tx.close();
		logger.info("insertion of source document finished");
		graphDb.index();
		logger.info("indexing finished");
	}

	public void removeSourceDocument(String sourceDocumentID) {
		// TODO Auto-generated method stub
		
	}
	
}


