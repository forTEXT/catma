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
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;

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
			SourceDocumentIndexer sourceDocumentIndexer = new SourceDocumentIndexer();
			sourceDocumentIndexer.index(
					session, 
					sourceDocument, 
					unseparableCharacterSequences, 
					userDefinedSeparatingCharacters, 
					locale);
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
		
		Session session = sessionFactory.openSession();
		try {
			TagReferenceIndexer tagReferenceIndexer = new TagReferenceIndexer();
			
			tagReferenceIndexer.index(
					session, 
					tagReferences, 
					sourceDocumentID, 
					userMarkupCollectionID, 
					tagLibrary);
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
	
	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList) throws Exception {
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		
		return phraseSearcher.search(documentIdList, phrase, termList);
	}

	public QueryResult searchTagDefinitionPath(
			List<String> documentIdList, List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) throws Exception {
		
		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher(sessionFactory);
		
		return tagSearcher.search(
				documentIdList, userMarkupCollectionIdList, tagDefinitionPath);
	}

	public QueryResult searchFreqency(
			List<String> documentIdList, 
			CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) {
		FrequencySearcher freqSearcher = new FrequencySearcher(sessionFactory);
		return freqSearcher.search(documentIdList, comp1, freq1, comp2, freq2);
	}

	
	public void close() {
		sessionFactory.close();
	}

}
