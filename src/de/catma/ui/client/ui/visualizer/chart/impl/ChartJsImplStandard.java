package de.catma.ui.client.ui.visualizer.chart.impl;

import com.google.gwt.core.client.JavaScriptObject;

import de.catma.ui.client.ui.visualizer.chart.ChartJs;

public class ChartJsImplStandard {
	
	public native ChartJs create(
			JavaScriptObject configuration) /*-{
		var chart = new $wnd.Highcharts.Chart(configuration);
   		return chart;
	}-*/;
}
