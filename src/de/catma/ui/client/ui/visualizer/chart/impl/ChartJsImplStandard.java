package de.catma.ui.client.ui.visualizer.chart.impl;

import com.google.gwt.core.client.JavaScriptObject;

import de.catma.ui.client.ui.visualizer.chart.ChartJs;

public class ChartJsImplStandard {
	
	public native ChartJs create(String targetSelector, double tickIntervalValue, JavaScriptObject seriesValues) /*-{
		var chart = new $wnd.Highcharts.Chart({
			chart: {
			    renderTo: targetSelector,
		    },
	        xAxis: {
	            tickInterval : tickIntervalValue
	        },
	        plotOptions: {
	            series: {
	                allowPointSelect: false
	            }
	        },
	        series: seriesValues
   		});
   		return chart;
	}-*/;
}
