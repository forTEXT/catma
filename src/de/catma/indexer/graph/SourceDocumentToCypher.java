package de.catma.indexer.graph;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.catma.document.source.FileOSType;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

public class SourceDocumentToCypher {

	
	public SourceDocumentToCypher() {
		
	}

	public String convert(SourceDocument sourceDocument) throws IOException {
		StringBuilder builder = new StringBuilder();
		
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

		
		builder.append("CREATE (sd:SourceDocument {localUri:'");
		builder.append(sourceDocument.getID());
		builder.append("',");
		builder.append("title:'");
		builder.append(sourceDocument.toString());
		builder.append("'})");
		builder.append(FileOSType.UNIX.getLineSeparator());
		
		int idx=0;
		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			String term = entry.getKey();
			List<TermInfo> termInfos = entry.getValue();
			
			builder.append("MERGE (t");
			builder.append(idx);
			builder.append(":Term {literal:'");
			builder.append(term);
			builder.append("'})");
			builder.append(FileOSType.UNIX.getLineSeparator());
			
			for (TermInfo ti : termInfos) {
				builder.append("MERGE (t");
				builder.append(idx);
				builder.append(")-[:IS_PART_OF");
				builder.append("{position:");
				builder.append(ti.getTokenOffset());
				builder.append(",");
				builder.append("range:[");
				builder.append(ti.getRange().getStartPoint());
				builder.append(",");
				builder.append(ti.getRange().getEndPoint());
				builder.append("]}]->(sd)");
				builder.append(FileOSType.UNIX.getLineSeparator());
			}
			idx++;
		}
		
		String prevTerm = null;
		
		for (String term : termExtractor.getTermsInOrder()) {
			if (prevTerm != null) {
				builder.append("MATCH (t1:Term {literal:'");
				builder.append(prevTerm);
				builder.append("'}),(t2:Term {literal:'");
				builder.append(term);
				builder.append("'}) MERGE (t1)-[:ADJACENT_TO {sourceDoc:'");
				builder.append(sourceDocument.getID());
				builder.append("'}]->(t2)");
				builder.append(FileOSType.UNIX.getLineSeparator());
			}
			prevTerm = term;
		}
		
//		MATCH (charlie:Person { name:'Charlie Sheen' }),(wallStreet:Movie { title:'Wall Street' })
//		MERGE (charlie)-[r:ACTED_IN]->(wallStreet)
		
		return builder.toString();
	}
	
}

