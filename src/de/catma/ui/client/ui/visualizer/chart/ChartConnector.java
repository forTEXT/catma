package de.catma.ui.client.ui.visualizer.chart;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.visualizer.chart.Chart;

@Connect(Chart.class)
public class ChartConnector extends AbstractComponentConnector {

	private ChartServerRpc chartServerRpc = RpcProxy.create(ChartServerRpc.class, this);
	
	public ChartConnector() {
		
		registerRpc(ChartClientRpc.class, new ChartClientRpc() {
			
			@Override
			public void init(String chartId, String configuration) {
				getWidget().init(chartId, configuration, new ChartClickListener() {
					
					@Override
					public void onClick(String seriesName, int x, int y) {
						chartServerRpc.onChartPointClick(seriesName, x, y);
					}
				});
			}
			
			@Override
			public void addSeries(String series) {
				getWidget().addSeries(series);
			}
			
			@Override
			public void setYAxisExtremes(int min, int max) {
				getWidget().setYAxisExtremes(min, max);
			}
			
			@Override
			public void setLenseZoom(double factor) {
				getWidget().setLenseZoom(factor);
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
