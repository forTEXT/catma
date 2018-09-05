package de.catma.indexer.indexbuffer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

public class IndexBufferManager {

	private ConcurrentHashMap<String, SourceDocument> indexedDocuments = new ConcurrentHashMap<>();
	
	private LoadingCache<String, SourceDocumentIndexBuffer> indices = 
			CacheBuilder
			.newBuilder()
			.maximumWeight(2000000)
			.weigher(new Weigher<String, SourceDocumentIndexBuffer>() {
				public int weigh(String key, SourceDocumentIndexBuffer value) {
					return value.getSize();
				};
			})
			.build(new CacheLoader<String, SourceDocumentIndexBuffer>() {

				@Override
				public SourceDocumentIndexBuffer load(String key) throws Exception {
					SourceDocument sourceDocument = indexedDocuments.get(key);
					List<String> unseparableCharacterSequences = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
								.getIndexInfoSet().getUnseparableCharacterSequences();
					List<Character> userDefinedSeparatingCharacters = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
								.getIndexInfoSet().getUserDefinedSeparatingCharacters();
					Locale locale = 
							sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
							.getIndexInfoSet().getLocale();
					
					TermExtractor termExtractor = 
							new TermExtractor(
								sourceDocument.getContent(), 
								unseparableCharacterSequences, 
								userDefinedSeparatingCharacters, 
								locale);
					
					final Map<String, List<TermInfo>> terms = termExtractor.getTerms();

					
					sourceDocument.unload();
					
					return new SourceDocumentIndexBuffer(key, sourceDocument.getLength(), terms);
				}
				
			});
	
	public IndexBufferManager() {
	}

	public SourceDocumentIndexBuffer get(String sourceDocumentId) throws IOException {
		if (indexedDocuments.containsKey(sourceDocumentId)) {
			try {
				return indices.get(sourceDocumentId);
			} catch (ExecutionException e) {
				throw new IOException(e);
			}
		}
		else {
			return null;
		}
	}
	
	public void remove(String sourceDocumentId) {
		indices.invalidate(sourceDocumentId);
		indexedDocuments.remove(sourceDocumentId);
	}
	
	public void add(SourceDocument sourceDocument, Map<String, List<TermInfo>> terms) throws IOException {
		indexedDocuments.put(
			sourceDocument.getID(), sourceDocument);
		indices.put(
				sourceDocument.getID(), 
				new SourceDocumentIndexBuffer(sourceDocument.getID(), sourceDocument.getLength(), terms));
	}
}
