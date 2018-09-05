package de.catma.ui.client.ui.zoomableverticallayout;

import com.vaadin.shared.communication.ClientRpc;

public interface ZoomableClientRpc extends ClientRpc {
	public void zoom(double factor);
}
