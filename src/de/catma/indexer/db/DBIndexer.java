package de.catma.indexer.db;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.jboss.logging.Logger;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagLibrary;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRowArray;

public class DBIndexer implements Indexer {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private SessionFactory sessionFactory; 
	private Configuration hibernateConfig;
	
	public DBIndexer() {
		hibernateConfig = new Configuration();
		hibernateConfig.configure(
				this.getClass().getPackage().getName().replace('.', '/') 
				+ "/hibernate.cfg.xml");
		
		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
		serviceRegistryBuilder.applySettings(hibernateConfig.getProperties());
		ServiceRegistry serviceRegistry = 
				serviceRegistryBuilder.buildServiceRegistry();
		
		sessionFactory = hibernateConfig.buildSessionFactory(serviceRegistry);
	}
	

	public void index(SourceDocument sourceDocument,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale)
			throws Exception {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			
			
			TermExtractor termExtractor = 
					new TermExtractor(
						sourceDocument.getContent(), 
						unseparableCharacterSequences, 
						userDefinedSeparatingCharacters, 
						locale);
			
			Map<String, List<TermInfo>> terms = termExtractor.getTerms();
			
			for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
				Term term = new Term(
						sourceDocument.getID(), 
						entry.getValue().size(), entry.getKey());
				session.save(term);
				
				for (TermInfo ti : entry.getValue()) {
					Position p = new Position(
						term,
						ti.getRange().getStartPoint(),
						ti.getRange().getEndPoint(),
						ti.getTokenOffset());
					session.save(p);
				}
			}
			
			session.getTransaction().commit();
			session.close();
		}
		catch (Exception e) {
			try {
				session.getTransaction().rollback();
			}
			catch (Throwable notOfInterest) {}
			session.close();
			throw e;
		}
	}

	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws Exception {
		// TODO Auto-generated method stub

	}
	
	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList) throws Exception {
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		
		return phraseSearcher.searchPhrase(documentIdList, phrase, termList);
	}

	public QueryResultRowArray searchTag(String tagPath, boolean isPrefixSearch)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		sessionFactory.close();
	}

}
