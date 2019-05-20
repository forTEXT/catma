package de.catma.ui.events.routing;

import de.catma.document.Corpus;
import de.catma.indexer.IndexedRepository;

public class RouteToAnalyzeNewEvent {
	private IndexedRepository project;
	private Corpus corpus;
	
	public RouteToAnalyzeNewEvent(IndexedRepository project, Corpus corpus) {
		super();
		this.project = project;
		this.corpus = corpus;
	}
	public IndexedRepository getProject() {
		return project;
	}
	public Corpus getCorpus() {
		return corpus;
	}
	

}
