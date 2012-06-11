package de.catma.queryengine.result.computation;


public class PlotBand {
	
	private String label;
	private double start;
	private double end;
	
	public PlotBand(String label, double start, double end) {
		this.label = label;
		this.start = start;
		this.end = end;
	}

	public String getLabel() {
		return label;
	}
	
	public double getStart() {
		return start;
	}
	
	public double getEnd() {
		return end;
	}
}
