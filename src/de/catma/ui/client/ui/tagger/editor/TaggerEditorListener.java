package de.catma.ui.client.ui.tagger.editor;

import java.util.List;

public interface TaggerEditorListener {
	public static enum TaggerEditorEventType {
		ADD,
		REMOVE,
		;
	}
	
	public void tagChanged(TaggerEditorEventType type, Object... args);
	public void tagsSelected(List<String> tagInstanceIDs);
}
