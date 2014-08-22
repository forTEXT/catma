package de.catma.ui.client.ui.visualizer.chart;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.visualizer.chart.Chart;

@Connect(Chart.class)
public class ChartConnector extends AbstractComponentConnector {
	
	public ChartConnector() {
		registerRpc(ChartClientRpc.class, new ChartClientRpc() {
			
			@Override
			public void init(String chartId, String configuration) {
				getWidget().init(chartId, configuration);
			}
		});
	}
	
	@Override
	protected ChartWidget createWidget() {
		return GWT.create(ChartWidget.class);
	}
	
	@Override
	public ChartWidget getWidget() {
		return (ChartWidget) super.getWidget();
	}
}
