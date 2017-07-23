package de.catma.queryengine.result.json;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;

public class JSONQueryResultBuilder {
	
	private final static class SourceDocInfo {
		public long size;
		public ContentInfoSet contentInfoSet;

		public SourceDocInfo(long size, ContentInfoSet contentInfoSet) {
			super();
			this.size = size;
			this.contentInfoSet = contentInfoSet;
		}
	}
	
	public enum Field {
		sourceDocumentId,
		sourceDocumentSize,
		startOffset,
		endOffset,
		phrase,
		annotationCollectionId,
		tagId,
		tagPath,
		tagVersion,
		annotationId,
		propertyId,
		propertyName,
		propertyValue, 
		sourceDocumentTitle,
	}
	
	public ArrayNode createJSONQueryResult(final QueryResult queryResult, final Repository repository) throws IOException {

		LoadingCache<String, SourceDocInfo> sourceDocInfoCache = 
				CacheBuilder.newBuilder().maximumSize(10).build(new CacheLoader<String, SourceDocInfo>() {
					
			@Override
			public SourceDocInfo load(String key) throws Exception {
				SourceDocument sd = repository.getSourceDocument(key);
				
				boolean unload = !sd.isLoaded();
				try {
					long size = sd.getLength();
					return new SourceDocInfo(size, sd.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet());
				}
				finally {
					if (unload) {
						sd.unload();
					}
				}
			}
		});

		JsonNodeFactory factory = JsonNodeFactory.instance;

		ArrayNode valuesArray = factory.arrayNode();

		for (QueryResultRow row : queryResult) {
			ObjectNode rowNode = factory.objectNode();
			addQueryResultRowFields(rowNode, row);
			if (row instanceof TagQueryResultRow) {
				for (Range range : ((TagQueryResultRow) row).getRanges()) {
					addTagQueryResultRowFields(rowNode, (TagQueryResultRow)row, range);
				}
			}
			else {
				rowNode.put(Field.startOffset.name(), row.getRange().getStartPoint());
				rowNode.put(Field.endOffset.name(), row.getRange().getEndPoint());
			}
			
			SourceDocInfo info;
			try {
				info = sourceDocInfoCache.get(row.getSourceDocumentId());
			} catch (ExecutionException e) {
				throw new IOException(e);
			}
			rowNode.put(Field.sourceDocumentSize.name(), info.size);
			rowNode.put(Field.sourceDocumentTitle.name(), info.contentInfoSet.getTitle());
			
			valuesArray.add(rowNode);
		}
		
		return valuesArray;
		
	}

	private void addTagQueryResultRowFields(ObjectNode rowNode, TagQueryResultRow row, Range range) {
		rowNode.put(Field.annotationCollectionId.name(), row.getMarkupCollectionId());
		rowNode.put(Field.tagId.name(),  row.getTagDefinitionId());
		rowNode.put(Field.tagPath.name(), row.getTagDefinitionPath());
		rowNode.put(Field.tagVersion.name(), row.getTagDefinitionVersion());
		rowNode.put(Field.annotationId.name(),  row.getTagInstanceId());
		rowNode.put(Field.propertyId.name(),  row.getPropertyDefinitionId());
		rowNode.put(Field.propertyName.name(),  row.getPropertyName());
		rowNode.put(Field.propertyValue.name(),  row.getPropertyValue());
		rowNode.put(Field.startOffset.name(), range.getStartPoint());
		rowNode.put(Field.endOffset.name(), range.getEndPoint());
	}
	

	private void addQueryResultRowFields(ObjectNode rowNode, QueryResultRow row) {
		rowNode.put(Field.sourceDocumentId.name(), row.getSourceDocumentId());
		rowNode.put(Field.phrase.name(), row.getPhrase());
//		rowNode.put(Field.markupCollectionId.name(), (String)null);
//		rowNode.put(Field.tagDefinitionId.name(),  (String)null);
//		rowNode.put(Field.tagDefinitionPath.name(), (String)null);
//		rowNode.put(Field.tagDefinitionVersion.name(), (String)null);
//		rowNode.put(Field.tagInstanceId.name(),  (String)null);
//		rowNode.put(Field.propertyDefinitionId.name(),  (String)null);
//		rowNode.put(Field.propertyName.name(),  (String)null);
//		rowNode.put(Field.propertyValue.name(),  (String)null);

	}
	
}
