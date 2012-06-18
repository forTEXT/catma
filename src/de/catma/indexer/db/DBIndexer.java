package de.catma.indexer.db;

import java.io.IOException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import de.catma.db.CloseableSession;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.Indexer;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.CloseSafe;

public class DBIndexer implements Indexer {
	
	private SessionFactory sessionFactory; 
	private Configuration hibernateConfig;
	private TagReferenceIndexer tagReferenceIndexer;

	public DBIndexer(String url, String user, String pass) {
		hibernateConfig = new Configuration();
		hibernateConfig.configure(
				this.getClass().getPackage().getName().replace('.', '/') 
				+ "/hibernate.cfg.xml");
		hibernateConfig.setProperty("hibernate.connection.username", user);
		hibernateConfig.setProperty("hibernate.connection.url",url);
		if ((pass != null) && (!pass.isEmpty())) {
			hibernateConfig.setProperty("hibernate.connection.password", pass);
		}

		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
		serviceRegistryBuilder.applySettings(hibernateConfig.getProperties());
		ServiceRegistry serviceRegistry = 
				serviceRegistryBuilder.buildServiceRegistry();
		
		sessionFactory = hibernateConfig.buildSessionFactory(serviceRegistry);
		tagReferenceIndexer = new TagReferenceIndexer();
	}
	

	public void index(SourceDocument sourceDocument)
			throws Exception {
		Session session = sessionFactory.openSession();
		try {
			SourceDocumentIndexer sourceDocumentIndexer = new SourceDocumentIndexer();
			sourceDocumentIndexer.index(
					session, 
					sourceDocument);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw e;
		}
	}

	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws Exception {
		
		Session session = sessionFactory.openSession();
		try {
			tagReferenceIndexer.index(
					session, 
					tagReferences, 
					sourceDocumentID, 
					userMarkupCollectionID, 
					tagLibrary);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw e;
		}
	}
	
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws Exception {
		Session session = sessionFactory.openSession();
		try {
			tagReferenceIndexer.removeUserMarkupCollection(
					session, userMarkupCollectionID);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw e;
		}
	}
	
	public void removeTagReferences(
			List<TagReference> tagReferences) throws Exception {
		Session session = sessionFactory.openSession();
		try {
			tagReferenceIndexer.removeTagReferences(
					session, tagReferences);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw e;
		}
	}
	
	public void reindex(TagsetDefinition tagsetDefinition,
			UserMarkupCollection userMarkupCollection, String sourceDocumentID)
			throws Exception {
		Session session = sessionFactory.openSession();
		try {
			tagReferenceIndexer.reindex(
					session, tagsetDefinition, userMarkupCollection, sourceDocumentID);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw e;
		}
	}
	
	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList) throws IOException {
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		
		return phraseSearcher.search(documentIdList, phrase, termList);
	}
	
	public QueryResult searchWildcardPhrase(List<String> documentIdList,
			List<String> termList) throws IOException {
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		
		
		return phraseSearcher.searchWildcard(documentIdList, termList);
	}

	public QueryResult searchTagDefinitionPath(List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) throws Exception {
		
		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher(sessionFactory);
		
		return tagSearcher.search(userMarkupCollectionIdList, tagDefinitionPath);
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
