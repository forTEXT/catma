package de.catma.ui.client.ui.visualizer;

import java.util.Date;

import com.google.gwt.user.client.ui.Composite;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;

import de.catma.ui.client.ui.visualizer.shared.DoubleTreeMessageAttribute;

public class VDoubleTree extends Composite implements Paintable {
	
	// The client side widget identifier 
	private String clientID;

	// Reference to the server connection object. 
	private ApplicationConnection serverConnection;

	private DoubleTreeWidget doubleTreeWidget;
	
	public VDoubleTree() {
		initComponents();
	}
	
    private void initComponents() {
    	doubleTreeWidget = new DoubleTreeWidget(new Date().getTime());
		initWidget(doubleTreeWidget);
	}

	/**
     * Called whenever an update is received from the server 
     */
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		// This call should be made first. 
		// It handles sizes, captions, tooltips, etc. automatically.
		if (client.updateComponent(this, uidl, true)) {
		    // If client.updateComponent returns true there has been no changes and we
		    // do not need to update anything.
			return;
		}

		// Save reference to server connection object to be able to send
		// user interaction later
		this.serverConnection = client;

		// Save the client side identifier (paintable id) for the widget
		this.clientID = uidl.getId();
		
		
		if (uidl.hasAttribute(DoubleTreeMessageAttribute.TREEDATA.name())) {
			KwicList kwicList = 
				KwicList.fromJSON(
					uidl.getStringAttribute(DoubleTreeMessageAttribute.TREEDATA.name()));
			
			doubleTreeWidget.setupFromArrays(
				kwicList.getPrefixes(), kwicList.getTokens(), kwicList.getPostfixes(),
				kwicList.isCaseSensitive()); 
		}
		
		if (uidl.hasAttribute(DoubleTreeMessageAttribute.WIDTH.name())) {
			doubleTreeWidget.setVisWidth(
				Integer.valueOf(
					uidl.getStringAttribute(
							DoubleTreeMessageAttribute.WIDTH.name())));
		}
	}
	
//	private void sendMessage(DoubleTreeMessageAttribute dtEventAttribute, String message) {
//		VConsole.log("sending message " + dtEventAttribute + "["+message+"] to the server");
//		serverConnection.updateVariable(
//				clientID, dtEventAttribute.name(), message, true);
//		
//	}



}
