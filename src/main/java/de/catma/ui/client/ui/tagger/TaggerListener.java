package de.catma.ui.client.ui.tagger;

import java.util.List;

import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public interface TaggerListener {
	public void tagInstanceAdded(String tagInstanceJson);
	public void log(String msg);
	public void tagInstanceSelected(String instanceIDJson);
	public void tagInstanceRemoved(String tagInstanceID);
	public void tagInstancesSelected(String tagInstanceIDsJson);
	public void contextMenuSelected(int x, int y);
	public void addComment(List<TextRange> ranges, int x, int y);
	public void editComment(ClientComment comment, int x, int y);
	public void removeComment(ClientComment comment);
	public void replyToComment(ClientComment comment, int x, int y);
	public void loadReplies(String uuid);
}
