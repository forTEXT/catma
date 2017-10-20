package de.catma.repository.jsonld;

import com.jsoniter.annotation.JsonProperty;

public class TagInstanceLdTarget {
	private String source;
	private RangeLd textPositionSelector;

	public TagInstanceLdTarget() {
		this.textPositionSelector = new RangeLd();
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@JsonProperty(to="TextPositionSelector")
	public RangeLd getTextPositionSelector() {
		return this.textPositionSelector;
	}

	@JsonProperty(from="TextPositionSelector")
	public void setTextPositionSelector(RangeLd textPositionSelector) {
		this.textPositionSelector = textPositionSelector;
	}
}
