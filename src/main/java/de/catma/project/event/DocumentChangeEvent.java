package de.catma.project.event;

import de.catma.document.source.SourceDocument;

public class DocumentChangeEvent {
	private final SourceDocument document;
	private final ChangeType changeType;

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
