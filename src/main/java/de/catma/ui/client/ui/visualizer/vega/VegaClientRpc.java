package de.catma.ui.client.ui.visualizer.vega;

import com.vaadin.shared.communication.ClientRpc;

public interface VegaClientRpc extends ClientRpc {
	public void reloadData();
}
