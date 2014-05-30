package de.catma.indexer.graph;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.schema.IndexCreator;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

public class SourceDocumentBatchInserter {

	public enum RelType implements RelationshipType {
		IS_PART_OF,
		ADJACENT_TO,
		HAS_POSITION,
		;
	}
	
	public SourceDocumentBatchInserter() {
		
	}

	public void insert(SourceDocument sourceDocument) throws IOException {
		
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

		
		BatchInserter inserter = 
				BatchInserters.inserter("C:/data/projects/catma/graphdb");
		
		Label sourceDocLabel = DynamicLabel.label( "SourceDocument" );
//		inserter.createDeferredSchemaIndex( so ).on( "name" ).create();
		Map<String, Object> properties = new HashMap<String, Object>();
		
		
		properties.put("localUri", sourceDocument.getID());
		properties.put("title", sourceDocument.toString());
		
		long sdNode = inserter.createNode(properties, sourceDocLabel);
		Label termLabel = DynamicLabel.label("Term");
		Label positionLabel = DynamicLabel.label("Position");
		
		properties.clear();
		Map<TermInfo,Long> termInfoToNodeId = new HashMap<TermInfo, Long>();
		TreeSet<TermInfo> orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
		
		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			String term = entry.getKey();
			List<TermInfo> termInfos = entry.getValue();
			
			properties.put("literal", term);
			long termNodeId = inserter.createNode(properties, termLabel);
			properties.clear();
			
			for (TermInfo ti : termInfos) {
				orderedTermInfos.add(ti);
				
				properties.put("position", ti.getTokenOffset());
				properties.put("range", new Integer[] {ti.getRange().getStartPoint(), ti.getRange().getEndPoint()});
				properties.put("literal", ti.getTerm());
				
				long positionNodeId = inserter.createNode(properties, positionLabel);
				termInfoToNodeId.put(ti, positionNodeId);
				inserter.createRelationship(termNodeId, positionNodeId, RelType.HAS_POSITION, Collections.<String,Object>emptyMap());
				inserter.createRelationship(termNodeId, sdNode, RelType.IS_PART_OF, Collections.<String, Object>emptyMap());
			}
		}

		
		
		TermInfo prevTi = null;
		properties.clear();
		
		for (TermInfo ti : orderedTermInfos) {
			if (prevTi != null) {
				properties.put("sourceDoc", sourceDocument.getID());
				inserter.createRelationship(
					termInfoToNodeId.get(prevTi), termInfoToNodeId.get(ti), 
					RelType.ADJACENT_TO, properties);
			}
			prevTi = ti;
		}
		IndexCreator termIndexCreator = inserter.createDeferredSchemaIndex(termLabel).on("literal");
		termIndexCreator.create();
		
		inserter.createDeferredSchemaIndex(termLabel).on("localUri").create();
		inserter.createDeferredSchemaIndex(termLabel).on("title").create();
		
		inserter.shutdown();
	}
	
}

