package de.catma.ui.repository;

import de.catma.document.source.SourceDocument;

public class MarkupCollectionItem {
	private String displayString;
	private boolean userMarkupCollectionItem = false;
	private String parentId;

	public MarkupCollectionItem(SourceDocument parent, String displayString) {
		this(parent, displayString, false);
	}
	
	public MarkupCollectionItem(
			SourceDocument parent,
			String displayString, boolean userMarkupCollectionItem) {
		this.parentId = parent.getID();
		this.displayString = displayString;
		this.userMarkupCollectionItem = userMarkupCollectionItem;
	}

	@Override
	public String toString() {
		return displayString;
	}
	
	public boolean isUserMarkupCollectionItem() {
		return userMarkupCollectionItem;
	}
	
	public String getParentId() {
		return parentId;
	}
}