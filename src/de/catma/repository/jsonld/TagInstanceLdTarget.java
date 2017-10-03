package de.catma.repository.jsonld;

public class TagInstanceLdTarget {
	public TagInstanceLdTarget(){
		this.TextPositionSelector = new RangeLd();
	}

	public String source;
	public RangeLd TextPositionSelector;
}
