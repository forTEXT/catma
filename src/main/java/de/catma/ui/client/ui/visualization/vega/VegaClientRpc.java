package de.catma.ui.client.ui.visualization.vega;

import com.vaadin.shared.communication.ClientRpc;

public interface VegaClientRpc extends ClientRpc {
	public void reloadData();
}
