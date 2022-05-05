package de.catma.project.event;

import de.catma.document.source.SourceDocumentReference;

public class DocumentChangeEvent {
	private final SourceDocumentReference docRef;
	private final ChangeType changeType;

	public DocumentChangeEvent(SourceDocumentReference docRef, ChangeType changeType) {
		super();
		this.docRef = docRef;
		this.changeType = changeType;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public SourceDocumentReference getDocument() {
		return docRef;
	}
}
