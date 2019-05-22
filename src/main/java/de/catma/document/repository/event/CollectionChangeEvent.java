package de.catma.document.repository.event;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CollectionChangeEvent {
	
	private UserMarkupCollectionReference collectionReference;
	private SourceDocument document;
	private ChangeType changeType;
	
	public CollectionChangeEvent(UserMarkupCollectionReference collectionReference, SourceDocument document,
			ChangeType changeType) {
		super();
		this.collectionReference = collectionReference;
		this.document = document;
		this.changeType = changeType;
	}
	
	public UserMarkupCollectionReference getCollectionReference() {
		return collectionReference;
	}

	public SourceDocument getDocument() {
		return document;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

}
