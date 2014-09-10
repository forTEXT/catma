package de.catma.ui.client.ui.visualizer.chart;

import com.vaadin.shared.communication.ServerRpc;

public interface ChartServerRpc extends ServerRpc {
	public void onChartPointClick(String seriesName, int x, int y);
}
