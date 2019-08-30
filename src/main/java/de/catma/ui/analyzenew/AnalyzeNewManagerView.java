package de.catma.ui.analyzenew;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.UI;

import de.catma.document.Corpus;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.CatmaApplication;
import de.catma.ui.tabbedview.TabbedView;

public class AnalyzeNewManagerView extends TabbedView {
	
	private EventBus eventBus;
	
	public AnalyzeNewManagerView(EventBus eventBus, IndexedRepository project) {
		super(() -> new AnalyzeNewView(new Corpus(), project, eventBus));	
		this.eventBus = eventBus;
	}
	
	public void analyzeNewDocuments(Corpus corpus, IndexedRepository repository) {
		try {
			addClosableTab(new AnalyzeNewView(corpus, repository, this.eventBus));	
		}	
		catch (Exception e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError("error initializing Analyzer", e);
		}
			
	}
}
