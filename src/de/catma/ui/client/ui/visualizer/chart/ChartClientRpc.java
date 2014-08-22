package de.catma.ui.client.ui.visualizer.chart;

import com.vaadin.shared.communication.ClientRpc;

public interface ChartClientRpc extends ClientRpc {
	public void init(String chartId, String configuration);
}
