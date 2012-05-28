package de.catma.indexer;

import java.io.IOException;
import java.util.List;

import de.catma.document.Range;
import de.catma.document.source.ISourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.tag.ITagLibrary;

public interface Indexer {
	public void index(
			ISourceDocument sourceDocument) throws Exception;
	
	public void index(
			List<TagReference> tagReferences,
			String sourceDocumentID,
			String userMarkupCollectionID,
			ITagLibrary tagLibrary) throws Exception;
	
	/**
	 * @param documentIdList
	 * @param phrase
	 * @param termList
	 * @return
	 */
	public QueryResult searchPhrase(
			List<String> documentIdList, 
			String phrase, List<String> termList) throws Exception;
	
	public QueryResult searchTagDefinitionPath(
			List<String> documentIdList, List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) throws Exception;
	
	public QueryResult searchFreqency(
			List<String> documentIdList, 
			CompareOperator comp1, int freq1, 
			CompareOperator comp2, int freq2);
	
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range,
	            int spanContextSize, SpanDirection direction) throws IOException;
	    	
	public QueryResult searchCollocation(
			QueryResult baseResult, QueryResult collocationConditionResult,
			int spanContextSize, SpanDirection direction) throws IOException;
	
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range);
	
	public void close();
}
