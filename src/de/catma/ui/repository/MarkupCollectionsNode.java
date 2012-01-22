package de.catma.ui.repository;

public class MarkupCollectionsNode {
	private String displayValue;

	public MarkupCollectionsNode(String displayValue) {
		super();
		this.displayValue = displayValue;
	}
	
	@Override
	public String toString() {
		return displayValue;
	}

}
