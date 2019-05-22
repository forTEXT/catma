package de.catma.document.repository.event;

import de.catma.document.source.SourceDocument;

public class DocumentChangeEvent {
	private SourceDocument document;
	private ChangeType changeType;

	public DocumentChangeEvent(SourceDocument document, ChangeType changeType) {
		super();
		this.document = document;
		this.changeType = changeType;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public SourceDocument getDocument() {
		return document;
	}
}
