package de.catma.ui.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusWidget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;

public class VApplicationStartedNotifier extends FocusWidget implements Paintable {

	private boolean started = false;
	
	public VApplicationStartedNotifier() {
		super(Document.get().createDivElement());
	}

	public void updateFromUIDL(final UIDL uidl, final ApplicationConnection client) {

		if (!started) {
			started = true;
			client.updateVariable(uidl.getId(), "AppStarted", true, true);
		}
	}

}
