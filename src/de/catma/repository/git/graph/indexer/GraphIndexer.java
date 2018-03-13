package de.catma.repository.git.graph.indexer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.impl.SchedulerRepository;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.Indexer;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.repository.git.graph.indexer.SourceDocumentIndexerJob.DataField;
import de.catma.tag.Property;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class GraphIndexer implements Indexer {
	
	private SourceDocumentIndexerJob sourceDocumentIndexer = new SourceDocumentIndexerJob();

	@Override
	public void index(SourceDocument sourceDocument, BackgroundService backgroundService) throws Exception {

//		sourceDocumentIndexer.index(sourceDocument);
	}

	@Override
	public void index(List<TagReference> tagReferences, String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSourceDocument(String sourceDocumentID) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagReferences(List<TagReference> tagReferences) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void reindex(TagsetDefinition tagsetDefinition, TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog,
			UserMarkupCollection userMarkupCollection) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public QueryResult searchPhrase(List<String> documentIdList, String phrase, List<String> termList, int limit)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchWildcardPhrase(List<String> documentIdList, List<String> termList, int limit)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchTagDefinitionPath(List<String> userMarkupCollectionIdList, String tagDefinitionPath)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchProperty(List<String> userMarkupCollectionIdList, String propertyName,
			String propertyValue, String tagValue) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchFreqency(List<String> documentIdList, CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchCollocation(QueryResult baseResult, QueryResult collocationConditionResult,
			int spanContextSize, SpanDirection direction) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateIndex(TagInstance tagInstance, Collection<Property> properties) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUserMarkupCollections(Collection<String> usermarkupCollectionIDs) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<TagDefinitionPathInfo> getTagDefinitionPathInfos(List<String> userMarkupCollectionIDs)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchTagDiff(List<String> relevantUserMarkupCollIDs, String propertyName, String tagPhrase)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
