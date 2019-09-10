package de.catma.ui.events.routing;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.Project;


public class RouteToAnnotateEvent {

	private final Project project;
	private SourceDocument document;
	private AnnotationCollectionReference collectionReference;
	

	public RouteToAnnotateEvent(Project project, SourceDocument document,
			AnnotationCollectionReference collectionReference) {
		super();
		this.project = project;
		this.document = document;
		this.collectionReference = collectionReference;
	}

	public RouteToAnnotateEvent(Project project) {
		this(project, null, null);
	}
	
	public Project getProject() {
		return project;
	}

	public SourceDocument getDocument() {
		return document;
	}

	public AnnotationCollectionReference getCollectionReference() {
		return collectionReference;
	}
	
	
}
