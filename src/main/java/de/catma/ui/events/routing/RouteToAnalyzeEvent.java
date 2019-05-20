package de.catma.ui.events.routing;

import de.catma.document.Corpus;
import de.catma.indexer.IndexedRepository;

public class RouteToAnalyzeEvent {
	private IndexedRepository project;
	private Corpus corpus;
	
	public RouteToAnalyzeEvent(IndexedRepository project, Corpus corpus) {
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
