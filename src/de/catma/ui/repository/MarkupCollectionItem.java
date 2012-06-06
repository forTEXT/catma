package de.catma.ui.repository;

public class MarkupCollectionItem {
	private String displayString;
	private boolean userMarkupCollectionItem = false;

	public MarkupCollectionItem(String displayString) {
		this(displayString, false);
	}
	
	public MarkupCollectionItem(
			String displayString, boolean userMarkupCollectionItem) {
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
}