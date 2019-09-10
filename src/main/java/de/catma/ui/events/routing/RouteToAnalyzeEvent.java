package de.catma.ui.events.routing;

import de.catma.document.corpus.Corpus;
import de.catma.indexer.IndexedProject;

public class RouteToAnalyzeEvent {
	private IndexedProject project;
	private Corpus corpus;
	
	public RouteToAnalyzeEvent(IndexedProject project, Corpus corpus) {
		super();
		this.project = project;
		this.corpus = corpus;
	}
	public IndexedProject getProject() {
		return project;
	}
	public Corpus getCorpus() {
		return corpus;
	}
	

}
