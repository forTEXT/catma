package de.catma.ui.client.ui.tagger;

public interface TaggerListener {
	public void tagInstanceAdded(String tagInstanceJson);
	public void log(String msg);
	public void tagInstanceSelected(String instanceIDJson);
	public void tagInstanceRemoved(String tagInstanceID);
	public void tagInstancesSelected(String tagInstanceIDsJson);
}
