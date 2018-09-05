package de.catma.ui.client.ui.tagger;

import com.vaadin.shared.communication.ClientRpc;

public interface TaggerClientRpc extends ClientRpc {
	public void setTaggerId(String taggerId);
	public void setPage(String page, int lineCount);
	public void removeTagInstances(String tagInstancesJson);
	public void addTagInstances(String tagInstancesJson);
	public void addTagInstanceWith(String tagDefinitionJson);
	public void setTagInstanceSelected(String tagInstanceId);
	public void setTraceSelection(boolean traceSelection);
	public void removeHighlights();
}
