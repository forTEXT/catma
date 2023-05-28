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
package de.catma.indexer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.TagLibrary;

public interface Indexer {

	public void index(
			SourceDocument sourceDocument, BackgroundService backgroundService) throws Exception;
	
	public void index(
			List<TagReference> tagReferences,
			String sourceDocumentID,
			String userMarkupCollectionID,
			TagLibrary tagLibrary) throws IOException;
	
	public void removeSourceDocument(String sourceDocumentID) throws IOException;
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws IOException;
	public void removeTagReferences(List<TagReference> tagReferences) throws IOException;

	
	/**
	 * @param queryId the ID of the Query
	 * @param documentIdList a list of SourceDocument {@link SourceDocument#getUuid() IDs}
	 * @param phrase the phrase to search for
	 * @param termList the terms of that phrase in writing order
	 * @param limit a limit for the amount of result rows
	 * @return occurrences of that phrase
	 * @throws Exception 
	 */
	public QueryResult searchPhrase(
			QueryId queryId, 
			List<String> documentIdList, 
			String phrase, List<String> termList, int limit) throws Exception;
	
	public QueryResult searchWildcardPhrase(
			QueryId queryId, 
			List<String> documentIdList,
			List<String> termList, int limit) throws Exception;
	
	public QueryResult searchTagDefinitionPath(
			QueryId queryId, 
			List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath);
	
	public QueryResult searchProperty(
			QueryId queryId, 
			List<String> userMarkupCollectionIdList,
			String propertyName, String propertyValue, String tagValue);
	
	public QueryResult searchFrequency(
			QueryId queryId, 
			List<String> documentIdList, 
			CompareOperator comp1, int freq1, 
			CompareOperator comp2, int freq2) throws IOException;
	
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range,
	            int spanContextSize, SpanDirection direction) throws IOException;
	    	
	public QueryResult searchCollocation(
			QueryId queryId, 
			QueryResult baseResult, QueryResult collocationConditionResult,
			int spanContextSize, SpanDirection direction) throws IOException;
		
	public void close();

	public QueryResult searchTagDiff(QueryId queryId, List<String> relevantUserMarkupCollIDs,
			String propertyName, String tagPhrase) throws IOException;

	public QueryResult searchCommentPhrase(
			QueryId queryId, 
			List<String> documentIdList,
			List<String> termList, int limit, List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) throws Exception;


}
