package de.catma.ui.module.analyze;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Range;
import de.catma.document.comment.Reply;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.CommentQueryResultRow;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ColorConverter;

public class CSVExportPropertiesAsColumnsFlatStreamSource implements StreamSource {
	
	private final Supplier<QueryResult> queryResultSupplier;
	private final Project project;
	private final LoadingCache<String, KwicProvider> kwicProviderCache;
	private final BackgroundServiceProvider backgroundServiceProvider;
	   
	public CSVExportPropertiesAsColumnsFlatStreamSource(
			Supplier<QueryResult> queryResultSupplier, Project project,
			LoadingCache<String, KwicProvider> kwicProviderCache, BackgroundServiceProvider backgroundServiceProvider) {
		super();
		this.queryResultSupplier = queryResultSupplier;
		this.project = project;
		this.kwicProviderCache = kwicProviderCache;
		this.backgroundServiceProvider = backgroundServiceProvider;
	}

	@Override
	public InputStream getStream() {
		final QueryResult queryResult = queryResultSupplier.get();
        final PipedInputStream in = new PipedInputStream();
        final UI ui = UI.getCurrent();
        final Lock lock = new ReentrantLock();
        final Condition sending  = lock.newCondition();
        lock.lock();

        backgroundServiceProvider.submit("csv-export", new DefaultProgressCallable<Void>() {
        	@Override
        	public Void call() throws Exception {
            	PipedOutputStream out = new PipedOutputStream(in);
            	OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
        		LoadingCache<String, String> colorCache = 
        				CacheBuilder.newBuilder().build(new CacheLoader<String, String>() {
        					@Override
        					public String load(String tagDefinitionId) throws Exception {
        						return "#"+ColorConverter.toHex(project.getTagManager().getTagLibrary().getTagDefinition(tagDefinitionId).getColor());
        					}
        				});
        		
        		// group all rows by their tagInstanceId
    			HashMap<String, QueryResultRowArray> rowsGroupedByTagInstance = 
    					new HashMap<String, QueryResultRowArray>();
    			TreeSet<String> propertyNames = new TreeSet<String>();
    			QueryResultRowArray untaggedRows = new QueryResultRowArray();
    			
    			for (QueryResultRow row : queryResult) {
    				
    				if (row instanceof TagQueryResultRow) {
    					TagQueryResultRow tRow = (TagQueryResultRow) row;
    					QueryResultRowArray rows = 
    							rowsGroupedByTagInstance.get(tRow.getTagInstanceId());
    					
    					if (rows == null) {
    						rows = new QueryResultRowArray();
    						rowsGroupedByTagInstance.put(tRow.getTagInstanceId(), rows);
    					}
    					rows.add(tRow);
    					if (tRow.getPropertyName() != null) {
    						propertyNames.add(tRow.getPropertyName());
    					}
    				}
    				else {
    					// in case we have a result which is not tag based we simply collect the rows for further processing
    					untaggedRows.add(row);
    				}
    			}
        		
                try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.builder().setDelimiter(';').build())) {

        			List<String> headerNames = new ArrayList<>();
        			// add common field headers
                    headerNames.addAll(List.of("Query ID", "Document ID", "Document Name", "Document length", "Keyword", "Keyword in context", "Start offset", "End offset",
                			"Collection ID", "Collection Name", "Tag", "Tag Version", "Tag Color", "Annotation ID"));

                    // add property field headers
                    if (!propertyNames.isEmpty()) {
                    	headerNames.addAll(propertyNames);
                    }
                    
                    // add comment fields
                    headerNames.addAll(List.of("Comment ID", "Comment/Reply", "Comment Author", "Reply count", "Reply ID"));
                    
                	csvPrinter.printRecord((Object[])CSVFormat.EXCEL.builder().setHeader(headerNames.toArray(new String[] {})).build().getHeader());
                	
                	// handle tag based rows 
                	for (QueryResultRowArray group : rowsGroupedByTagInstance.values()) {
                    	Iterator<QueryResultRow> peekIter = group.iterator();
                    	if (peekIter.hasNext()) {
                    		// get a master Row for the common fields
                    		QueryResultRow peekRow = peekIter.next();
                    		
    	            		TagQueryResultRow tRow = (TagQueryResultRow) peekRow;
        	            	KwicProvider kwicProvider = kwicProviderCache.get(tRow.getSourceDocumentId());

	            			// get all property values grouped by their name for this group
	            			Map<String, Set<String>> propertyValuesByPropertyName = 
	            					group.stream()
	            					.map(r -> (TagQueryResultRow)r)
	            					.filter(r -> r.getPropertyName() != null && r.getPropertyValue() != null)
	            					.collect(
	            							Collectors.groupingBy(
	            									TagQueryResultRow::getPropertyName, 
	            									TreeMap::new, 
	            									Collectors.mapping(
	            											TagQueryResultRow::getPropertyValue, Collectors.toSet())));
        	            	List<Range> mergedRanges = 
    	    						Range.mergeRanges(new TreeSet<>((tRow).getRanges()));
    	            		for (Range range : mergedRanges) {
    	            			KeywordInSpanContext kwic = kwicProvider.getKwic(range, 5);
    	            			List<Object> values = new ArrayList<>();
    	            			// add common field values
    	            			values.addAll(List.of(
    	            					tRow.getQueryId().toSerializedString(),
    	            					tRow.getSourceDocumentId(),
        	            				kwicProvider.getSourceDocumentName(),
        	            				kwicProvider.getDocumentLength(),
        	            				kwic.getKeyword(),
        	            				kwic.toString(),
        	            				range.getStartPoint(),
        	            				range.getEndPoint(),
        	            				tRow.getMarkupCollectionId(),        	            				
        	            				kwicProvider.getSourceDocumentReference().getUserMarkupCollectionReference(tRow.getMarkupCollectionId()).toString(),
        	            				tRow.getTagDefinitionPath(),
        	            				tRow.getTagDefinitionVersion(),
        	            				colorCache.get(tRow.getTagDefinitionId()),
        	            				tRow.getTagInstanceId()));
    	            			
    	            			// add property field values
    	            			for (String propertyName : propertyNames) {
    	            				if (propertyValuesByPropertyName.containsKey(propertyName)) {
    	            					values.add(propertyValuesByPropertyName.get(propertyName).stream().collect(Collectors.joining(",")));
    	            				}
    	            				else {
    	            					values.add("");
    	            				}
    	            			}
    	            			
    	            			csvPrinter.printRecord(values.toArray());
    	            		}
                    	}
                	}
                	
    	            for (QueryResultRow row : untaggedRows) {
    	            	KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
    	            	if (row instanceof CommentQueryResultRow) {
    	            		CommentQueryResultRow cRow = (CommentQueryResultRow)row;

    	    				List<Range> mergedRanges = 
    	    						Range.mergeRanges(new TreeSet<>((cRow).getRanges()));
    	            		for (Range range : mergedRanges) {
    	            			KeywordInSpanContext kwic = kwicProvider.getKwic(range, 5);
    	            			
    	            			List<Object> values = new ArrayList<>();

    	            			// add common fields
        	            		values.addAll(List.of(
        	            				row.getQueryId().toSerializedString(),
        	            				row.getSourceDocumentId(),
        	            				kwicProvider.getSourceDocumentName(),
        	            				kwicProvider.getDocumentLength(),
        	            				kwic.getKeyword(),
        	            				kwic.toString(),
        	            				range.getStartPoint(),
        	            				range.getEndPoint()
        	            		));
        	            		
        	            		// add empty common tag fields
        	            		values.addAll(List.of("","","","","",""));
        	            		// add empty property fields
        	            		propertyNames.forEach(ign -> values.add(""));
        	            		
        	            		// add comment fields
        	            		values.addAll(List.of(
        	            				cRow.getComment().getUuid(),
        	            				cRow.getComment().getBody(),
        	            				cRow.getComment().getUsername(),
        	            				cRow.getComment().getReplyCount(),
        	            				""
	            				));
        	            		
        	            		csvPrinter.printRecord(values);
        	            		
        	            		for (Reply reply : cRow.getComment().getReplies()) {
        	            			List<Object> replyValues = new ArrayList<>();
            	            		replyValues.addAll(List.of(
            	            				row.getQueryId().toSerializedString(),
            	            				row.getSourceDocumentId(),
            	            				kwicProvider.getSourceDocumentName(),
            	            				kwicProvider.getDocumentLength(),
            	            				kwic.getKeyword(),
            	            				kwic.toString(),
            	            				range.getStartPoint(),
            	            				range.getEndPoint()
            	            		));
            	            		
            	            		
            	            		// add empty common tag fields
            	            		replyValues.addAll(List.of("","","","","",""));
            	            		// add empty property fields
            	            		propertyNames.forEach(ign -> replyValues.add(""));
            	            		
            	            		// add comment fields
            	            		replyValues.addAll(List.of(
            	            				cRow.getComment().getUuid(),
            	            				reply.getBody(),
            	            				reply.getUsername(),
            	            				0,
            	            				reply.getUuid()
    	            				));
            	            		csvPrinter.printRecord(replyValues);
        	            		}
    	            		}    	            		
    	            	}
    	            	else {
	            			KeywordInSpanContext kwic = kwicProvider.getKwic(row.getRange(), 5);
    	            		csvPrinter.printRecord(
    	            				row.getQueryId().toSerializedString(),
    	            				row.getSourceDocumentId(),
    	            				kwicProvider.getSourceDocumentName(),
    	            				kwicProvider.getDocumentLength(),
    	            				kwic.getKeyword(),
    	            				kwic.toString(),
    	            				row.getRange().getStartPoint(),
    	            				row.getRange().getEndPoint());
    	            	}
    	
    	                csvPrinter.flush();
    	                lock.lock();
    	                try {
    	                	sending.signal();
    	                }
    	                finally {
    	                	lock.unlock();
    	                }
    	            }
                }

        		return null; //intended
        	}
		}, 
        new ExecutionListener<Void>() {
			@Override
			public void done(Void result) {
				// noop
			}
			@Override
			public void error(Throwable t) {
				((ErrorHandler) ui).showAndLogError("Error exporting data to CSV", t);
			}
		});
        
        // waiting on the background thread to send data to the pipe
		try {
			sending.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error while waiting on CSV export", e);
		}
		finally {
			lock.unlock();
		}


        
        return in;
	}
}
