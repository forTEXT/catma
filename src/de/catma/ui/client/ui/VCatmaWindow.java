package de.catma.ui.client.ui;

import com.google.gwt.user.client.Window;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.ui.VWindow;

public class VCatmaWindow extends VWindow {
	
	private boolean stayOnTop;

	public static enum EventAttribute {
		enableScrolling,
		stayOnTop,
		;
	}
	
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		super.updateFromUIDL(uidl, client);
		// This call should be made first. 
		// It handles sizes, captions, tooltips, etc. automatically.
		if (client.updateComponent(this, uidl, true)) {
		    // If client.updateComponent returns true there has been no changes and we
		    // do not need to update anything.
			return;
		}
		
		if (uidl.hasAttribute(EventAttribute.enableScrolling.name())) {
			Window.enableScrolling(
					uidl.getBooleanAttribute(EventAttribute.enableScrolling.name()));
		}
		
		if (uidl.hasAttribute(EventAttribute.stayOnTop.name())) {
			stayOnTop = 
					uidl.getBooleanAttribute(EventAttribute.stayOnTop.name());
		}
	}
	
	@Override
	protected void setZIndex(int zIndex) {
		if (!stayOnTop) {
			super.setZIndex(zIndex);
		}
	}
	
}
