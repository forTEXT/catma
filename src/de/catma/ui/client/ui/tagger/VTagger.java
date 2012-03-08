/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */   
package de.catma.ui.client.ui.tagger;

import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;

import de.catma.ui.client.ui.tagger.editor.TaggerEditor;
import de.catma.ui.client.ui.tagger.editor.TaggerEditorListener;
import de.catma.ui.client.ui.tagger.shared.TaggerMessageAttribute;
import de.catma.ui.client.ui.tagger.shared.TagInstance;


/**
 * @author marco.petris@web.de
 *
 */
public class VTagger extends Composite implements Paintable {

	// The client side widget identifier 
	private String clientID;

	// Reference to the server connection object. 
	private ApplicationConnection serverConnection;
	
	private TaggerEditor taggerEditor;
	private TagInstanceJSONSerializer tagInstanceJSONSerializer;
	
	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VTagger() {
		super();
		this.tagInstanceJSONSerializer = new TagInstanceJSONSerializer();
		initComponents();
		
	}
	
	private void initComponents() {
		taggerEditor = new TaggerEditor(new TaggerEditorListener() {
			public void tagChanged(TaggerEditorEventType type, Object... args) {
				switch(type) {
				
					case ADD : {
						TagInstance tagInstance = (TagInstance)args[0];
						sendMessage(
								TaggerMessageAttribute.TAGINSTANCE_ADD, 
								tagInstanceJSONSerializer.toJSONObject(tagInstance));
						break;
					}
				
					case REMOVE : {
						String tagInstanceID  = (String)args[0];
						boolean reportToServer = (Boolean)args[1];
						if (reportToServer) {
							sendMessage(
								TaggerMessageAttribute.TAGINSTANCE_REMOVE, 
								tagInstanceID);
						}
						break;
					}
				
				}
				
			}
		});
		initWidget(taggerEditor);
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

		if (uidl.hasAttribute(TaggerMessageAttribute.PAGE_SET.name())) {
			VConsole.log("setting page content");
			taggerEditor.setHTML(
				new HTML(
					uidl.getStringAttribute(TaggerMessageAttribute.PAGE_SET.name())));
		}


		if (uidl.hasAttribute(TaggerMessageAttribute.TAGINSTANCE_CLEAR.name()) 
				&& uidl.getBooleanAttribute(
						TaggerMessageAttribute.TAGINSTANCE_CLEAR.name())) {
			//TODO: gets no longer sent by the server, might be obsolete
			taggerEditor.clearTagInstances();
		}
		
		if (uidl.hasAttribute(TaggerMessageAttribute.TAGINSTANCES_ADD.name())) {
			List<TagInstance> tagInstances = 
					tagInstanceJSONSerializer.fromJSONArray(
						uidl.getStringAttribute(
							TaggerMessageAttribute.TAGINSTANCES_ADD.name()));
			for (TagInstance tagInstance : tagInstances) {
				VConsole.log("got tag instance from server (show): " + tagInstance);
				taggerEditor.addTagInstance(tagInstance);
			}
		}
		
		if (uidl.hasAttribute(TaggerMessageAttribute.TAGINSTANCES_REMOVE.name())) {
			List<TagInstance> tagInstances = 
					tagInstanceJSONSerializer.fromJSONArray(
						uidl.getStringAttribute(
							TaggerMessageAttribute.TAGINSTANCES_REMOVE.name()));
			
			for (TagInstance tagInstance : tagInstances) {
				VConsole.log("got TagInstance from server (hide): " + tagInstance);
				taggerEditor.removeTagInstance(
						tagInstance.getInstanceID(), 
						false); //don't report to server
			}
		}
		
		if (uidl.hasAttribute(TaggerMessageAttribute.TAGDEFINITION_SELECTED.name())) {
			String color = 
				uidl.getStringAttribute(
						TaggerMessageAttribute.TAGDEFINITION_SELECTED.name());
			taggerEditor.createAndAddTagIntance(color);
		}
		
	}
	
	public void logToServer(String logMsg) {
		sendMessage(TaggerMessageAttribute.LOGMESSAGE, logMsg);
	}

	private void sendMessage(TaggerMessageAttribute taggerEventAttribute, String message) {
		serverConnection.updateVariable(
				clientID, taggerEventAttribute.name(), message, true);
		
	}

}
