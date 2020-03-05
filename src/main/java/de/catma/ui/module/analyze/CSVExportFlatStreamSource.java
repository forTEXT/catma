package de.catma.ui.module.analyze;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ColorConverter;

public class CSVExportFlatStreamSource implements StreamSource {
	
	private final Supplier<QueryResult> queryResultSupplier;
	private final Project project;
	private final LoadingCache<String, KwicProvider> kwicProviderCache;
	private final BackgroundServiceProvider backgroundServiceProvider;
	   
	public CSVExportFlatStreamSource(
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
        		
                try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(';'))) {
    	            for (QueryResultRow row : queryResult) {
    	            	KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
    	            	if (row instanceof TagQueryResultRow) {
    	            		TagQueryResultRow tRow = (TagQueryResultRow) row;
    	            		for (Range range : ((TagQueryResultRow) row).getRanges()) {
    	            			KeywordInSpanContext kwic = kwicProvider.getKwic(range, 5);
        	            		csvPrinter.printRecord(
        	            				row.getQueryId().toSerializedString(),
        	            				row.getSourceDocumentId(),
        	            				kwicProvider.getSourceDocumentName(),
        	            				kwicProvider.getDocumentLength(),
        	            				kwic.getKeyword(),
        	            				kwic.toString(),
        	            				range.getStartPoint(),
        	            				range.getEndPoint(),
        	            				tRow.getMarkupCollectionId(),
        	            				kwicProvider.getSourceDocument().getUserMarkupCollectionReference(tRow.getMarkupCollectionId()).toString(),
        	            				tRow.getTagDefinitionPath(),
        	            				tRow.getTagDefinitionVersion(),
        	            				colorCache.get(tRow.getTagDefinitionId()),
        	            				tRow.getTagInstanceId(),
        	            				tRow.getPropertyDefinitionId(),
        	            				tRow.getPropertyName(),
        	            				tRow.getPropertyValue());
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
				((ErrorHandler)ui).showAndLogError("Error export data to CSV!", t);
			}
		});
        
        // waiting on the background thread to send data to the pipe
        try {
	        try {
				sending.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error while waiting on CSV export!", e1);
			}
        }
        finally {
       		lock.unlock();
        }

        
        return in;
	}
}
