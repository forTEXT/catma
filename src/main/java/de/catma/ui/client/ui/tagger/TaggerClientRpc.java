package de.catma.ui.client.ui.tagger;

import com.vaadin.shared.communication.ClientRpc;

public interface TaggerClientRpc extends ClientRpc {
	public void setTaggerId(String taggerId);
	public void setPage(String page, int lineCount, String comments);
	public void removeTagInstances(String tagInstancesJson);
	public void addTagInstances(String tagInstancesJson);
	public void addTagInstanceWith(String tagDefinitionJson);
	public void setTagInstanceSelected(String tagInstanceId);
	public void setTraceSelection(boolean traceSelection);
	public void removeHighlights();
	public void scrollLineToVisible(String lineId);
	public void addComment(String clientCommentJson);
	public void updateComment(String uuid, String body, int startPos);
	public void removeComment(String uuid, int startPos);
	public void setReplies(String uuid, int startPos, String repliesJsonArrayString);
}
