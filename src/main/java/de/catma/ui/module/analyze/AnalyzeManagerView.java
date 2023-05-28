package de.catma.ui.module.analyze;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.UI;

import de.catma.document.corpus.Corpus;
import de.catma.indexer.IndexedProject;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.tabbedview.TabbedView;

public class AnalyzeManagerView extends TabbedView {
	
	private EventBus eventBus;
	
	public AnalyzeManagerView(EventBus eventBus, IndexedProject project) {
		super(() -> new AnalyzeView(new Corpus(), project, eventBus));	
		this.eventBus = eventBus;
	}
	
	public void analyzeNewDocuments(Corpus corpus, IndexedProject repository) {
		try {
			addClosableTab(new AnalyzeView(corpus, repository, this.eventBus));	
		}	
		catch (Exception e) {
			((CatmaApplication) UI.getCurrent()).showAndLogError("Error initializing analyzer", e);
		}
			
	}
}
