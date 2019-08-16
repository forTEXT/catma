package de.catma.ui.analyzenew;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.ui.UI;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.KwicProvider;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzenew.AnalyzeNewView.CloseListener;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.tabbedview.TabbedView;

public class AnalyzeNewManagerView extends TabbedView {
	EventBus eventBus;

	
	public AnalyzeNewManagerView(EventBus eventBus, IndexedRepository repository) {
		super(
				Messages.getString("temporary caption"));	
		this.eventBus = eventBus;
					
	}
	
	public void analyzeNewDocuments(Corpus corpus, IndexedRepository repository) {
		try {
			AnalyzeNewView analyzeNewView = new AnalyzeNewView(corpus, repository, getKwicProviderCache(repository), new CloseListener() {
				@Override
				public void closeRequest(AnalyzeNewView analyzeNewView) {
		
						onTabClose(analyzeNewView);
					}			
			});	
			
			String caption= null;
			String substring=null;
			String substring2= null;
			int documents=corpus.getSourceDocuments().size();
			int collections=corpus.getUserMarkupCollectionRefs().size();
			
	 if(documents==1) {
		 substring = "1 document, ";
	 }else {
		 substring = documents+" documents, ";
	 } 
	 if(collections==1) {
		 substring2="1 collection";	 
	 }else {
		 substring2= collections+ " collections";
	 }
		caption= substring+substring2;
			addClosableTab(analyzeNewView, caption);		
		}	
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError("error initializing Analyzer", e);
		}
			
	}
	
	private LoadingCache<String, KwicProvider> getKwicProviderCache(Repository repository) {
		LoadingCache<String, KwicProvider> kwicProviderCache = CacheBuilder.newBuilder().maximumSize(10)
				.build(new CacheLoader<String, KwicProvider>() {

					@Override
					public KwicProvider load(String key) throws Exception {
	
						 SourceDocument sd= repository.getSourceDocument(key);
						 KwicProvider kwicProvider =  new KwicProvider(sd);
						 sd.unload();
						 return kwicProvider;
								 
								 
					}
				});	
		return kwicProviderCache;
	}

}
