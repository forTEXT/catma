package de.catma.ui.client.ui.tagmanager;

import com.vaadin.shared.communication.ServerRpc;

public interface ColorFieldServerRpc extends ServerRpc {
	public void colorChanged(String hexColor);
}
