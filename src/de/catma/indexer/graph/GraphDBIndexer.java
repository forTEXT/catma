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
package de.catma.indexer.graph;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.Indexer;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.indexer.TermInfo;
import de.catma.indexer.db.TagDefinitionSearcher;
import de.catma.indexer.db.TagReferenceIndexer;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.Property;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class GraphDBIndexer implements Indexer {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TagReferenceIndexer tagReferenceIndexer;
	private SourceDocumentIndexer sourceDocumentIndexer;
	
	public GraphDBIndexer(Map<String, Object> properties) {
		tagReferenceIndexer = new TagReferenceIndexer();
		sourceDocumentIndexer = new SourceDocumentIndexer();
	}
	
	public void index(SourceDocument sourceDocument, BackgroundService backgroundService)
			throws IOException {
		
		sourceDocumentIndexer.index(sourceDocument, backgroundService);
	}

	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws IOException {
		tagReferenceIndexer.index(
				tagReferences, 
				sourceDocumentID, 
				userMarkupCollectionID, 
				tagLibrary);
	}
	
	public void removeSourceDocument(String sourceDocumentID) throws IOException {
		sourceDocumentIndexer.removeSourceDocument(sourceDocumentID);
	}
	
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws IOException {
		tagReferenceIndexer.removeUserMarkupCollection(userMarkupCollectionID);
	}
	
	public void removeTagReferences(
			List<TagReference> tagReferences) throws IOException {
		tagReferenceIndexer.removeTagReferences(tagReferences);
	}
	
	public void reindex(TagsetDefinition tagsetDefinition,
			TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog,
			UserMarkupCollection userMarkupCollection)
			throws IOException {
		logger.info(
			"reindexing tagsetdefinition " + tagsetDefinition 
			+ " in umc " + userMarkupCollection);
		
		tagReferenceIndexer.reindex(
				tagsetDefinition, tagsetDefinitionUpdateLog, 
				userMarkupCollection);
	}
	
	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList, int limit) throws IOException {
		try {
			PhraseSearcher phraseSearcher = new PhraseSearcher();
			
			return phraseSearcher.search(documentIdList, phrase, termList, limit);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public QueryResult searchWildcardPhrase(List<String> documentIdList,
			List<String> termList, int limit) throws IOException {
		try {
			PhraseSearcher phraseSearcher = new PhraseSearcher();
			
			return phraseSearcher.searchWildcard(documentIdList, termList, limit);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}

	public QueryResult searchTagDefinitionPath(List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) throws IOException {
		
		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher();
	
		return tagSearcher.search(userMarkupCollectionIdList, tagDefinitionPath);
	}
	
	public QueryResult searchProperty(
			List<String> userMarkupCollectionIdList,
			String propertyName, String propertyValue, String tagValue) throws IOException {

		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher();
		return tagSearcher.searchProperties(
				userMarkupCollectionIdList, 
				propertyName, propertyValue, tagValue);
	}

	public QueryResult searchFreqency(
			List<String> documentIdList, 
			CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) throws IOException {
		FrequencySearcher freqSearcher = new FrequencySearcher();
		return freqSearcher.search(documentIdList, comp1, freq1, comp2, freq2);
	}

	public SpanContext getSpanContextFor(
			String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		CollocationSearcher collocationSearcher = 
				new CollocationSearcher();
		
		return collocationSearcher.getSpanContextFor(
				sourceDocumentId, range, spanContextSize, direction);
	}
	
	public QueryResult searchCollocation(QueryResult baseResult,
			QueryResult collocationConditionResult, int spanContextSize,
			SpanDirection direction) throws IOException {
		try {
			CollocationSearcher collocationSearcher = 
					new CollocationSearcher();
			return collocationSearcher.search(
				baseResult, collocationConditionResult, spanContextSize, direction);
		}
		catch (Exception ne) {
			throw new IOException(ne);
		}
	}
	
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) throws IOException {
		CollocationSearcher collocationSearcher = 
				new CollocationSearcher();
		return collocationSearcher.getTermInfosFor(sourceDocumentId, range);
	}

	
	public void updateIndex(TagInstance tagInstance, Collection<Property> properties) 
			throws IOException {
		for (Property property : properties) {
			tagReferenceIndexer.reindexProperty(tagInstance, property);
		}
	}
	

	public void removeUserMarkupCollections(
			Collection<String> userMarkupCollectionIDs) throws IOException {
		//too expensive, is done during maintenance
//		for (String userMarkupColl : userMarkupCollectionIDs) {
//			removeUserMarkupCollection(userMarkupColl);
//		}
		
	}
	
	public List<TagDefinitionPathInfo> getTagDefinitionPathInfos(List<String> userMarkupCollectionIDs) 
			throws IOException {
		try {
			TagDefinitionSearcher tagDefinitionSearcher = 
					new TagDefinitionSearcher();
			return tagDefinitionSearcher.getTagDefinitionPathInfos(userMarkupCollectionIDs);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public QueryResult searchTagDiff(List<String> relevantUserMarkupCollIDs,
			String propertyName, String tagPhrase) throws IOException {

		TagDefinitionSearcher tagSearcher = new TagDefinitionSearcher();
		return tagSearcher.searchTagDiff(
				relevantUserMarkupCollIDs, 
				propertyName, tagPhrase);
	}
	
	public void close() {
		// noop
	}

}
