package de.catma.ui.tagger.client.ui;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public interface HasTagEventHandlers extends HasHandlers {
	HandlerRegistration addTagEventHandler( TagEventHandler handler);
}
