package de.catma.ui.client.ui.tagger;

import com.vaadin.shared.communication.ClientRpc;

public interface TaggerClientRpc extends ClientRpc {
	public void setTaggerId(String taggerId);
	public void setPage(String page);
	public void removeTagInstances(String tagInstancesJson);
	public void addTagInstances(String tagInstancesJson);
	public void addTagInstanceWith(String tagDefinitionJson);
	public void highlight(String textRangeJson);
}
