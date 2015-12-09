package de.catma.ui.client.ui.visualizer.chart.impl;

import com.google.gwt.core.client.JavaScriptObject;

import de.catma.ui.client.ui.visualizer.chart.ChartClickListener;
import de.catma.ui.client.ui.visualizer.chart.ChartJs;

public class ChartJsImplStandard {
	
	public native ChartJs create(
			JavaScriptObject configuration, ChartClickListener chartClickListener) /*-{
			
		$wnd.__chartClickListener = function() {
			chartClickListener.@de.catma.ui.client.ui.visualizer.chart.ChartClickListener::onClick(Ljava/lang/String;II)(
				this.series.name, this.x, this.y);
		};
		
		configuration.plotOptions.series.point.events.click = $wnd.__chartClickListener;
		
		var chart = new $wnd.Highcharts.Chart(configuration);
   		return chart;
	}-*/;

	public native void addSeries(JavaScriptObject chart, JavaScriptObject series) /*-{
		chart.addSeries(series);
	}-*/;
	
	public native void setYAxisExtremes(JavaScriptObject chart, int min, int max) /*-{
		chart.yAxis[0].setExtremes(min, max);
	}-*/;

}
