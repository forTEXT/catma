package de.catma.ui.events.routing;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;


public class RouteToAnnotateEvent {

	private final Repository project;
	private SourceDocument document;
	private UserMarkupCollectionReference collectionReference;
	

	public RouteToAnnotateEvent(Repository project, SourceDocument document,
			UserMarkupCollectionReference collectionReference) {
		super();
		this.project = project;
		this.document = document;
		this.collectionReference = collectionReference;
	}

	public RouteToAnnotateEvent(Repository project) {
		this(project, null, null);
	}
	
	public Repository getProject() {
		return project;
	}

	public SourceDocument getDocument() {
		return document;
	}

	public UserMarkupCollectionReference getCollectionReference() {
		return collectionReference;
	}
	
	
}
