package de.catma.ui.client.ui.visualization.vega;

import java.util.List;

import com.vaadin.shared.communication.ServerRpc;

import de.catma.ui.client.ui.visualization.vega.shared.SelectedQueryResultRow;

public interface VegaServerRpc extends ServerRpc {
	public void onUserSelection(List<SelectedQueryResultRow> selection);
}
