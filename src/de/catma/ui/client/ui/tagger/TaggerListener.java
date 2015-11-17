package de.catma.ui.client.ui.tagger;

public interface TaggerListener {
	public void tagInstanceAdded(String tagInstanceJson);
	public void log(String msg);
	public void tagInstancesSelected(String instanceIDsJson);
	public void tagInstanceRemoved(String tagInstanceID);
}
