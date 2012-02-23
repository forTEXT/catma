package de.catma.ui.client.ui.tagger.editor;

public interface TaggerEditorListener {
	public static enum TaggerEditorEventType {
		ADD,
		REMOVE,
		;
	}
	
	public void tagChanged(TaggerEditorEventType type, Object... args);
}
