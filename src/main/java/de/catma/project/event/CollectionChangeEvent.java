package de.catma.project.event;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;

public class CollectionChangeEvent {
	
	private AnnotationCollectionReference collectionReference;
	private SourceDocument document;
	private ChangeType changeType;
	
	public CollectionChangeEvent(AnnotationCollectionReference collectionReference, SourceDocument document,
			ChangeType changeType) {
		super();
		this.collectionReference = collectionReference;
		this.document = document;
		this.changeType = changeType;
	}
	
	public AnnotationCollectionReference getCollectionReference() {
		return collectionReference;
	}

	public SourceDocument getDocument() {
		return document;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

}
