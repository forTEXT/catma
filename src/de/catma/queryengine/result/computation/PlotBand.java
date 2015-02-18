package de.catma.queryengine.result.computation;

import de.catma.util.ColorConverter;

public class PlotBand<X extends Comparable<X>> {

	private X startPosition;
	private X endPosition;
	private String label;
	private String hexColor;
	
	public PlotBand(X startPosition, X endPosition, String label) {
		super();
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.label = label;
		this.hexColor = ColorConverter.randomHex();
	}

	public X getStartPosition() {
		return startPosition;
	}

	public X getEndPosition() {
		return endPosition;
	}

	public String getLabel() {
		return label;
	}

	public String getHexColor() {
		return hexColor;
	}
	
}
