package de.catma.ui.client.ui.tagger;

import com.vaadin.shared.communication.ServerRpc;

public interface TaggerServerRpc extends ServerRpc {
	public void tagInstanceAdded(String tagInstanceJson);
	public void log(String msg);
	public void tagInstanceSelected(String instanceIDLineIDJson);
	public void tagInstancesSelected(String tagInstanceIDsJson);
	public void contextMenuSelected(int x, int y);
	public void addComment(String textRanges, int x, int y);
	public void editComment(String uuid, int x, int y);
	public void removeComment(String uuid);
}
