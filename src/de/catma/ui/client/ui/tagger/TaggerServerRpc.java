package de.catma.ui.client.ui.tagger;

import com.vaadin.shared.communication.ServerRpc;

public interface TaggerServerRpc extends ServerRpc {
	public void tagInstanceAdded(String tagInstanceJson);
	public void log(String msg);
	public void tagInstanceSelected(String instanceIDsJson);
}
