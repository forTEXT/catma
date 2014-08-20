package de.catma.ui.client.ui.visualizer.chart;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.shared.GWT;

import de.catma.ui.client.ui.visualizer.chart.impl.ChartJsImplStandard;
import de.catma.ui.client.ui.visualizer.chart.shared.ChartOptions;

public class ChartJs extends JavaScriptObject {
	
	
	protected ChartJs() {
	}
	
	private static ChartJsImplStandard impl = GWT.create(ChartJsImplStandard.class);
	
	public static ChartJs create(
			String targetSelector, ChartOptions chartOptions) {
		return impl.create(
				targetSelector, 
				chartOptions.title, 
				chartOptions.tickInterval, 
				JsonUtils.safeEval(chartOptions.series));
	}
}
