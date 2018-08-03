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
import java.util.Collection;
import java.util.List;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.Property;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

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
	public void reindex(
			TagsetDefinition tagsetDefinition, 
			TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog,
			UserMarkupCollection userMarkupCollection) throws IOException;

	
	/**
	 * @param documentIdList a list of SourceDocument {@link SourceDocument#getID() IDs}
	 * @param phrase the phrase to search for
	 * @param termList the terms of that phrase in writing order
	 * @param limit a limit for the amount of result rows
	 * @return occurrences of that phrase
	 * @throws IOException
	 */
	public QueryResult searchPhrase(
			List<String> documentIdList, 
			String phrase, List<String> termList, int limit) throws IOException;
	
	public QueryResult searchWildcardPhrase(List<String> documentIdList,
			List<String> termList, int limit) throws IOException;
	
	public QueryResult searchTagDefinitionPath(
			List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) throws IOException;
	
	public QueryResult searchProperty(
			List<String> userMarkupCollectionIdList,
			String propertyName, String propertyValue, String tagValue) throws IOException;
	
	public QueryResult searchFreqency(
			List<String> documentIdList, 
			CompareOperator comp1, int freq1, 
			CompareOperator comp2, int freq2) throws IOException;
	
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range,
	            int spanContextSize, SpanDirection direction) throws IOException;
	    	
	public QueryResult searchCollocation(
			QueryResult baseResult, QueryResult collocationConditionResult,
			int spanContextSize, SpanDirection direction) throws IOException;
	
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) throws IOException;
	
	public void close();

	public void updateIndex(TagInstance tagInstance, Collection<Property> properties) throws IOException;

	public void removeUserMarkupCollections(Collection<String> usermarkupCollectionIDs) throws IOException;

	public List<TagDefinitionPathInfo> getTagDefinitionPathInfos(
			List<String> userMarkupCollectionIDs) throws IOException;

	public QueryResult searchTagDiff(List<String> relevantUserMarkupCollIDs,
			String propertyName, String tagPhrase) throws IOException;


}
