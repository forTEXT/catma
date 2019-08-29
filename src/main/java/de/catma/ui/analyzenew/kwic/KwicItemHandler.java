package de.catma.ui.analyzenew.kwic;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;

public class KwicItemHandler {
	
	private int contextSize = 5; 

	private LoadingCache<String, KwicProvider> kwicProviderCache;
	private LoadingCache<QueryResultRow, KeywordInSpanContext> spanContextCache;
	
	public KwicItemHandler(LoadingCache<String, KwicProvider> kwicProviderCache) {
		super();
		this.kwicProviderCache = kwicProviderCache;
		this.spanContextCache = CacheBuilder.newBuilder().build(new CacheLoader<QueryResultRow, KeywordInSpanContext>() {
			@Override
			public KeywordInSpanContext load(QueryResultRow row) throws Exception {
				KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
				return kwicProvider.getKwic(row.getRange(), contextSize);
			}
		});
	}

	public String getDocumentName(QueryResultRow row) {
		String name = "N/A";
		try {
			KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
			if (kwicProvider != null) {
				name = kwicProvider.getSourceDocumentName();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return name;
	}
	
	public String getBackwardContext(QueryResultRow row) {
		String backwardContext = "N/A";
		try {
			backwardContext = spanContextCache.get(row).getBackwardContext();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return backwardContext;		
	}
	
	public String getForwardContext(QueryResultRow row) {
		String forwardContext = "N/A";
		try {
			forwardContext = spanContextCache.get(row).getForwardContext();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return forwardContext;		
	}	
	
	public String getKeyword(QueryResultRow row) {
		return AnnotatedTextProvider.shorten(
			row.getPhrase(), AnnotatedTextProvider.LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH);
	}
	
	public String getKeywordDescription(QueryResultRow row) {
		return row.getPhrase();
	}
	
	
	public String getTagPath(QueryResultRow row) {
		if (row instanceof TagQueryResultRow) {
			return ((TagQueryResultRow) row).getTagDefinitionPath();
		}
		return null;
	}
	
	public String getCollectionName(QueryResultRow row) {
		if (row instanceof TagQueryResultRow) {
			try {
				String documentId = row.getSourceDocumentId();
				String collectionId =  ((TagQueryResultRow) row).getMarkupCollectionId();
				UserMarkupCollectionReference colRef = 
					kwicProviderCache.get(documentId).getSourceDocument().getUserMarkupCollectionReference(collectionId);
				String collectionName = colRef.getName();
				
				return collectionName;
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "N/A";
			}
		}
		return null;		
	}
	
	public String getPropertyName(QueryResultRow row) {
		if (row instanceof TagQueryResultRow) {
			return ((TagQueryResultRow) row).getPropertyName();
		}
		
		return null;
	}
	
	public String getPropertyValue(QueryResultRow row) {
		if (row instanceof TagQueryResultRow) {
			return AnnotatedTextProvider.shorten(
				((TagQueryResultRow) row).getPropertyValue(), 
				AnnotatedTextProvider.LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH);
		}
		
		return null;
	}
	
	public String getPropertyValueDescription(QueryResultRow row) {
		if (row instanceof TagQueryResultRow) {
			return ((TagQueryResultRow) row).getPropertyValue();
		}
		
		return null;
	}

	public boolean containsSearchInput(QueryResultRow row, String searchInput) {
		if (getDocumentName(row).contains(searchInput)) {
			return true;
		}
		
		if (getBackwardContext(row).contains(searchInput)) {
			return true;
		}
		
		if (getForwardContext(row).contains(searchInput)) {
			return true;
		}		
		
		if (getKeywordDescription(row).contains(searchInput)) {
			return true;
		}
		
		String tagPath = getTagPath(row);
		if (tagPath != null && tagPath.contains(searchInput)) {
			return true;
		}
		
		String collection = getCollectionName(row);
		if (collection != null && collection.contains(searchInput)) {
			return true;
		}

		String propertyname = getPropertyName(row);
		if (propertyname != null && propertyname.contains(searchInput)) {
			return true;
		}

		String propertvalue = getPropertyValueDescription(row);
		if (propertvalue != null && propertvalue.contains(searchInput)) {
			return true;
		}
		
		return false;
	}	
}
