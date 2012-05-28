package de.catma.indexer.db;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Session;

import de.catma.document.source.ISourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

class SourceDocumentIndexer {
	
	public void index(Session session, ISourceDocument sourceDocument)
			throws Exception {
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
		
		session.beginTransaction();
		
		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			DBTerm term = new DBTerm(
					sourceDocument.getID(), 
					entry.getValue().size(), entry.getKey());
			session.save(term);
			
			for (TermInfo ti : entry.getValue()) {
				DBPosition p = new DBPosition(
					term,
					ti.getRange().getStartPoint(),
					ti.getRange().getEndPoint(),
					ti.getTokenOffset());
				session.save(p);
			}
		}
		
		session.getTransaction().commit();
	}

}
