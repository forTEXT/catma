package de.catma.indexer.graph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
		properties.clear();
		Map<String,Long> termToNodeId = new HashMap<String, Long>();
		
		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			String term = entry.getKey();
			List<TermInfo> termInfos = entry.getValue();
			
			properties.put("literal", term);
			long termNodeId = inserter.createNode(properties, termLabel);
			termToNodeId.put(term, termNodeId);
			properties.clear();
			
			for (TermInfo ti : termInfos) {
				properties.put("position", ti.getTokenOffset());
				properties.put("range", new Integer[] {ti.getRange().getStartPoint(), ti.getRange().getEndPoint()});
				
				inserter.createRelationship(termNodeId, sdNode, RelType.IS_PART_OF, properties);
			}
		}

		
		
		String prevTerm = null;
		properties.clear();
		
		for (String term : termExtractor.getTermsInOrder()) {
			if (prevTerm != null) {
				properties.put("sourceDoc", sourceDocument.getID());
				inserter.createRelationship(
					termToNodeId.get(prevTerm), termToNodeId.get(term), 
					RelType.ADJACENT_TO, properties);
			}
			prevTerm = term;
		}
		IndexCreator termIndexCreator = inserter.createDeferredSchemaIndex(termLabel).on("literal");
		termIndexCreator.create();
		
		inserter.createDeferredSchemaIndex(termLabel).on("localUri").create();
		inserter.createDeferredSchemaIndex(termLabel).on("title").create();
		
		inserter.shutdown();
	}
	
}

