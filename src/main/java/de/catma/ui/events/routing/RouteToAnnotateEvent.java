package de.catma.ui.events.routing;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;


public class RouteToAnnotateEvent {

	private final Project project;
	private SourceDocumentReference documentRef;
	private AnnotationCollectionReference collectionReference;
	

	public RouteToAnnotateEvent(Project project, SourceDocumentReference documentRef,
			AnnotationCollectionReference collectionReference) {
		super();
		this.project = project;
		this.documentRef = documentRef;
		this.collectionReference = collectionReference;
	}

	public RouteToAnnotateEvent(Project project) {
		this(project, null, null);
	}
	
	public Project getProject() {
		return project;
	}

	public SourceDocumentReference getDocument() {
		return documentRef;
	}

	public AnnotationCollectionReference getCollectionReference() {
		return collectionReference;
	}
	
	
}
