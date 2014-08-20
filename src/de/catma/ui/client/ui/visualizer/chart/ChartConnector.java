package de.catma.ui.client.ui.visualizer.chart;

import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.client.ui.visualizer.chart.shared.ChartOptions;
import de.catma.ui.visualizer.chart.Chart;

@Connect(Chart.class)
public class ChartConnector extends AbstractComponentConnector {
	
	public ChartConnector() {
		registerRpc(ChartClientRpc.class, new ChartClientRpc() {
			
			@Override
			public void init(ChartOptions chartOptions) {
				getWidget().init(chartOptions);
			}
		});
	}
	@Override
	public ChartWidget getWidget() {
		return (ChartWidget) super.getWidget();
	}
}
