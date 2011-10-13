package de.catma.ui.tagger.client.ui;

import com.google.gwt.event.shared.EventHandler;


public interface TagEventHandler extends EventHandler {
	public void onTagEvent(TagEvent event);
}
