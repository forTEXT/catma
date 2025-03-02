package de.catma.ui.module.analyze.visualization.kwic;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.comment.Comment;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.CommentQueryResultRow;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider;
import de.catma.ui.util.Cleaner;

public class KwicItemHandler {
	private Logger logger = Logger.getLogger(KwicItemHandler.class.getName());
	
	private LoadingCache<String, KwicProvider> kwicProviderCache;
	private LoadingCache<QueryResultRow, KeywordInSpanContext> spanContextCache;
	private Project project;
	
	public KwicItemHandler(Project project, LoadingCache<String, KwicProvider> kwicProviderCache, Supplier<Integer> contextSizeSupplier) {
		super();
		this.project = project;
		this.kwicProviderCache = kwicProviderCache;
		this.spanContextCache = CacheBuilder.newBuilder().build(new CacheLoader<QueryResultRow, KeywordInSpanContext>() {
			@Override
			public KeywordInSpanContext load(QueryResultRow row) throws Exception {
				KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
				return kwicProvider.getKwic(row.getRange(), contextSizeSupplier.get());
			}
		});
	}
	
	public void updateContextSize() {
		spanContextCache.invalidateAll();
	}

	public String getDocumentName(QueryResultRow row) {
		String name = "N/A";
		try {
			KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
			if (kwicProvider != null) {
				name = kwicProvider.getSourceDocumentName();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error retrieving document name for " + row, e);
		}
		return name;
	}
	
	public String getBackwardContext(QueryResultRow row) {
		String backwardContext = "N/A";
		try {
			backwardContext = spanContextCache.get(row).getBackwardContext();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error retrieving backward context for " + row, e);
		}
		return backwardContext;		
	}
	
	public String getForwardContext(QueryResultRow row) {
		String forwardContext = "N/A";
		try {
			forwardContext = spanContextCache.get(row).getForwardContext();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error retrieving forward context for " + row, e);
		}
		return forwardContext;		
	}	
	
	public String getKeyword(QueryResultRow row, int contextSize) {
		if (row instanceof TagQueryResultRow) {
			TagQueryResultRow tRow = (TagQueryResultRow)row;
			TagDefinition tagDefinition = 
					project.getTagManager().getTagLibrary().getTagDefinition(tRow.getTagDefinitionId());
			try {
				return AnnotatedTextProvider.buildAnnotatedText(
						new ArrayList<>(tRow.getRanges()), 
						kwicProviderCache.get(tRow.getSourceDocumentId()), 
						tagDefinition,
						contextSize);
			} catch (ExecutionException e) {
				logger.log(Level.SEVERE, "Error retrieving keyword for " + row, e);
			}
		}
		
		return AnnotatedTextProvider.shorten(
			row.getPhrase(), AnnotatedTextProvider.LARGE_MAX_ANNOTATED_KEYWORD_DISPLAY_LENGTH);
	}
	
	public String getKeywordDescription(QueryResultRow row, int contextSize) {
		if (row instanceof TagQueryResultRow) {
			TagQueryResultRow tRow = (TagQueryResultRow)row;
			TagDefinition tagDefinition = 
					project.getTagManager().getTagLibrary().getTagDefinition(tRow.getTagDefinitionId());

			try {
				return AnnotatedTextProvider.buildAnnotatedKeywordInContext(
						new ArrayList<>(tRow.getRanges()), 
						kwicProviderCache.get(tRow.getSourceDocumentId()), 
						tagDefinition, 
						tRow.getTagDefinitionPath(),
						contextSize);
			} catch (ExecutionException e) {
				logger.log(Level.SEVERE, "Error retrieving keyword description for " + row, e);
			}
		}
		
		if (row instanceof CommentQueryResultRow) {
			CommentQueryResultRow cRow = (CommentQueryResultRow)row;
			Comment comment = cRow.getComment();
			
			return AnnotatedTextProvider.buildCommentedKeyword(row.getPhrase(), comment);
		}
		
		return Cleaner.clean(row.getPhrase());
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
				AnnotationCollectionReference colRef = 
					kwicProviderCache.get(documentId).getSourceDocumentReference().getUserMarkupCollectionReference(collectionId);
				String collectionName = colRef.getName();
				
				return collectionName;
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "Error retrieving collection name for " + row, e);
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

	public boolean containsSearchInput(QueryResultRow row, String searchInput, int contextSize) {
		searchInput = searchInput.toLowerCase();
		
		if (getDocumentName(row).toLowerCase().contains(searchInput)) {
			return true;
		}
		
		if (getBackwardContext(row).toLowerCase().contains(searchInput)) {
			return true;
		}
		
		if (getForwardContext(row).toLowerCase().contains(searchInput)) {
			return true;
		}		
		
		if (getKeywordDescription(row, contextSize).toLowerCase().contains(searchInput)) {
			return true;
		}
		
		String tagPath = getTagPath(row);
		if (tagPath != null && tagPath.toLowerCase().contains(searchInput)) {
			return true;
		}
		
		String collection = getCollectionName(row);
		if (collection != null && collection.toLowerCase().contains(searchInput)) {
			return true;
		}

		String propertyname = getPropertyName(row);
		if (propertyname != null && propertyname.toLowerCase().contains(searchInput)) {
			return true;
		}

		String propertvalue = getPropertyValueDescription(row);
		if (propertvalue != null && propertvalue.toLowerCase().contains(searchInput)) {
			return true;
		}
		
		return false;
	}

	public String getKeywordStyle(QueryResultRow row) {
		try {
			if (spanContextCache.get(row).isRightToLeft()) {
				return "kwic-panel-keyword-rtl";
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error retrieving keyword style for " + row, e);
		}
		
		return "kwic-panel-keyword";
	}

	public String getBackwardContextStyle(QueryResultRow row) {
		try {
			if (spanContextCache.get(row).isRightToLeft()) {
				return "kwic-panel-backwardctx-rtl";
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error retrieving backward context style for " + row, e);
		}
		
		return "kwic-panel-backwardctx";
	}
	
	public String getForwardContextStyle(QueryResultRow row) {
		try {
			if (spanContextCache.get(row).isRightToLeft()) {
				return "kwic-panel-forwardctx-rtl";
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error retrieving forward context style for " + row, e);
		}
		
		return "kwic-panel-forwardctx";
	}
	
	public LoadingCache<String, KwicProvider> getKwicProviderCache() {
		return kwicProviderCache;
	}
}
