package de.catma.ui.analyzer;

import de.catma.ui.tabbedview.TabbedView;

public class VisualizationManagerView extends TabbedView {
	
	private LineChartView lineChartView;

	public VisualizationManagerView(String source) {
		super("No visualizations available");
		initTabs(source);
	}

	private void initTabs(String source) {
		lineChartView = new LineChartView(source);
		addTab(lineChartView, "Distribution Graph");
	}

	
}
