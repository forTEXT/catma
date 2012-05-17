package de.catma.ui.analyzer;

import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;
import de.catma.ui.tabbedview.TabbedView;

public class AnalyzerManagerView extends TabbedView {
	
	public AnalyzerManagerView() {
		super("To analyze documents and collections please do the following: ");
	}

	public void analyzeDocuments(Corpus corpus, Repository repository) {
		//TODO: make equal titles distinguishable
		AnalyzerView analyzerView = 
				new AnalyzerView(corpus, repository);
		
		addClosableTab(analyzerView, (corpus == null)? "All documents" : corpus.toString() );
	}
}