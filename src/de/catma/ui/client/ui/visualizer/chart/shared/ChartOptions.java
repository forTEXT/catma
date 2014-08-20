package de.catma.ui.client.ui.visualizer.chart.shared;

public class ChartOptions {
	public String title;
	public double tickInterval; 
	public String series;
	
	public ChartOptions() {
	}
	
	public ChartOptions(String title, double tickInterval, String series) {
		super();
		this.title = title;
		this.tickInterval = tickInterval;
		this.series = series;
	}
	
	
}
