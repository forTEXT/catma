package de.catma.indexer.indexbuffer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.catma.indexer.TermInfo;

public class IndexBufferManager {

	private ConcurrentHashMap<String, SourceDocumentIndexBuffer> indexedDocuments = new ConcurrentHashMap<>();
	
	public IndexBufferManager() {
	}

	public SourceDocumentIndexBuffer get(String sourceDocumentId) {
		return indexedDocuments.get(sourceDocumentId);
	}
	
	public void remove(String sourceDocumentId) {
		indexedDocuments.remove(sourceDocumentId);
	}
	
	public void add(String sourceDocumentId, Map<String, List<TermInfo>> terms) {
		indexedDocuments.put(
			sourceDocumentId, new SourceDocumentIndexBuffer(sourceDocumentId, terms));
	}
}
