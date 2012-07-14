package de.catma.ui.analyzer;

import java.util.HashSet;

import com.vaadin.ui.Component;

import de.catma.document.Corpus;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.analyzer.AnalyzerView.CloseListener;
import de.catma.ui.tabbedview.TabbedView;

public class AnalyzerManagerView extends TabbedView {
	
	public AnalyzerManagerView() {
		super("To analyze documents and collections please do the following: " +
				"Open a Repository with the Repository Manager and use the " +
				"analyze menu items in the corpora or source documents " +
				"sections or the analyze button of the Tagger.");
	}

	public void analyzeDocuments(Corpus corpus, IndexedRepository repository) {
		AnalyzerView analyzerView = 
				new AnalyzerView(corpus, repository, new CloseListener() {
					
					public void closeRequest(AnalyzerView analyzerView) {
						onTabClose(analyzerView);
					}
				});
		
		HashSet<String> captions = new HashSet<String>();
		
		for (Component c : this) {
			captions.add(getCaption(c));
		}
		
		String base = (corpus == null)? "All documents" : corpus.toString();
		String caption = base;
		
		int captionIndex = 1;
		while (captions.contains(caption)) {
			caption = base + "("+captionIndex+")";
			captionIndex++;
		}

		addClosableTab(analyzerView, caption);
	}
}
