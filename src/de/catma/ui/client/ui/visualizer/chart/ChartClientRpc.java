package de.catma.ui.client.ui.visualizer.chart;

import com.vaadin.shared.communication.ClientRpc;

import de.catma.ui.client.ui.visualizer.chart.shared.ChartOptions;

public interface ChartClientRpc extends ClientRpc {
	public void init(ChartOptions chartOptions);
}
