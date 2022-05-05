package de.catma.project.event;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;

public class CollectionChangeEvent {
	
	private final AnnotationCollectionReference collectionReference;
	private final SourceDocumentReference sourceDocumentReference;
	private final ChangeType changeType;
	
	public CollectionChangeEvent(AnnotationCollectionReference collectionReference, SourceDocumentReference documentReference,
			ChangeType changeType) {
		super();
		this.collectionReference = collectionReference;
		this.sourceDocumentReference = documentReference;
		this.changeType = changeType;
	}
	
	public AnnotationCollectionReference getCollectionReference() {
		return collectionReference;
	}

	public SourceDocumentReference getDocument() {
		return sourceDocumentReference;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

}
