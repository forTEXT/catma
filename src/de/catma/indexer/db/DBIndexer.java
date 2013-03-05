/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.indexer.db;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.db.CloseableSession;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerPropertyKey;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.CloseSafe;

public class DBIndexer implements Indexer {
	
	private SessionFactory sessionFactory; 
	private TagReferenceIndexer tagReferenceIndexer;
	private SourceDocumentIndexer sourceDocumentIndexer;

	public DBIndexer(Map<String, Object> properties) {
		this.sessionFactory = 
				(SessionFactory)properties.get(
						IndexerPropertyKey.SessionFactory.name());
		tagReferenceIndexer = new TagReferenceIndexer();
		sourceDocumentIndexer = new SourceDocumentIndexer();
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void index(SourceDocument sourceDocument)
			throws Exception {
		Session session = sessionFactory.openSession();
		try {
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
	
	public void index(Session session, List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws Exception {
		tagReferenceIndexer.index(
				session, 
				tagReferences, 
				sourceDocumentID, 
				userMarkupCollectionID, 
				tagLibrary);
	}
	
	public void removeSourceDocument(String sourceDocumentID) throws Exception {
		Session session = sessionFactory.openSession();
		try {
			sourceDocumentIndexer.removeSourceDocument(
					session, sourceDocumentID);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw e;
		}
	}
	
	public void removeSourceDocument(Session session, String sourceDocumentID) throws Exception {
		sourceDocumentIndexer.removeSourceDocument(
				session, sourceDocumentID);
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
	
	public void removeUserMarkupCollection(
			Session session, String userMarkupCollectionID) throws Exception {
		tagReferenceIndexer.removeUserMarkupCollection(
				session, userMarkupCollectionID);
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
			Set<byte[]> deletedTagDefinitionUuids,
			UserMarkupCollection userMarkupCollection, String sourceDocumentID)
			throws IOException {
		Session session = sessionFactory.openSession();
		try {
			tagReferenceIndexer.reindex(
					session, tagsetDefinition, deletedTagDefinitionUuids, 
					userMarkupCollection, sourceDocumentID);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw new IOException(e);
		}
	}
	
	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList, int limit) throws IOException {
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		
		return phraseSearcher.search(documentIdList, phrase, termList, limit);
	}
	
	public QueryResult searchWildcardPhrase(List<String> documentIdList,
			List<String> termList, int limit) throws IOException {
		PhraseSearcher phraseSearcher = new PhraseSearcher(sessionFactory);
		
		
		return phraseSearcher.searchWildcard(documentIdList, termList, limit);
	}

	public QueryResult searchTagDefinitionPath(List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) throws Exception {
		
		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher(sessionFactory);
		
		return tagSearcher.search(userMarkupCollectionIdList, tagDefinitionPath);
	}
	
	public QueryResult searchProperty(Set<String> propertyDefinitionIDs,
			String propertyName, String propertyValue) {

		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher(sessionFactory);
		
		return tagSearcher.searchProperties(propertyDefinitionIDs, propertyName, propertyValue);
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

	
	public void updateIndex(TagInstance tagInstance, Property property) 
			throws IOException {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			tagReferenceIndexer.reIndexProperty(session, tagInstance, property);
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session, true));
			throw new IOException(e);
		}
		
	}
	
	
	public void close() { /*noop sessionfactory is closed by repository*/ }

}
