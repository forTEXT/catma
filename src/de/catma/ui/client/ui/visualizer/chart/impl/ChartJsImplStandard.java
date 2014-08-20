package de.catma.ui.client.ui.visualizer.chart.impl;

import com.google.gwt.core.client.JavaScriptObject;

import de.catma.ui.client.ui.visualizer.chart.ChartJs;

public class ChartJsImplStandard {
	
	public native ChartJs create(
			String targetSelector,
			String title,
			double tickIntervalValue, 
			JavaScriptObject seriesValues) /*-{
		var chart = new $wnd.Highcharts.Chart({
			chart: {
			    renderTo: targetSelector,
		    },
	        xAxis: {
	            tickInterval : tickIntervalValue
	            max: 100
	        },
	        plotOptions: {
	            series: {
	                allowPointSelect: false
	            }
	        },
	        title: {
            	text: title
        	},
        	yAxis: {
	            title: {
	                text: 'Occurrences'
	            }
	        },
	        series: seriesValues
   		});
   		return chart;
	}-*/;
}
