package de.catma.indexer.db;

import java.io.IOException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.indexer.Indexer;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.TagLibrary;

public class DBIndexer implements Indexer {
	
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
	

	public void index(SourceDocument sourceDocument)
			throws Exception {
		Session session = sessionFactory.openSession();
		try {
			SourceDocumentIndexer sourceDocumentIndexer = new SourceDocumentIndexer();
			sourceDocumentIndexer.index(
					session, 
					sourceDocument);
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

	public SpanContext getSpanContextFor(
			String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		CollocationSearcher collocationSearcher = 
				new CollocationSearcher(sessionFactory);
		
		return collocationSearcher.getSpanContextFor(
				sourceDocumentId, range, spanContextSize, direction);
	}
	
	public QueryResult searchCollocation(QueryResult baseResult,
			QueryResult collocationConditionResult, int spanContextSize,
			SpanDirection direction) throws IOException {
		CollocationSearcher collocationSearcher = 
				new CollocationSearcher(sessionFactory);
		return collocationSearcher.search(
			baseResult, collocationConditionResult, spanContextSize, direction);
	}
	
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) {
		CollocationSearcher collocationSearcher = 
				new CollocationSearcher(sessionFactory);
		return collocationSearcher.getTermInfosFor(sourceDocumentId, range);
	}
	
	public void close() {
		sessionFactory.close();
	}

}
