package de.catma.indexer.db;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.indexer.db.model.DBPosition;
import de.catma.indexer.db.model.DBTerm;

class SourceDocumentIndexer {
	
	void index(Session session, SourceDocument sourceDocument)
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

	void removeSourceDocument(Session session, String sourceDocumentID) {
		SQLQuery sqlQuery = session.createSQLQuery(
				"delete p from CatmaIndex.position p join CatmaIndex.term t on p.termID = t.termID " 
				+ " where t.documentID = '" + sourceDocumentID 
				+ "'");
		
		boolean tranStartedLocally = false;
		if (!session.getTransaction().isActive()) {
			session.beginTransaction();
			tranStartedLocally = true;
		}
		sqlQuery.executeUpdate();
		sqlQuery = session.createSQLQuery(
					"delete t from CatmaIndex.term t" 
					+ " where t.documentID = '" + sourceDocumentID 
					+ "'");
		sqlQuery.executeUpdate();
		
		if (tranStartedLocally) {
			session.getTransaction().commit();
		}
	}

}
