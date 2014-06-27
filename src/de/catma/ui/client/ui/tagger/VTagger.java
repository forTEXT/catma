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
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;

import de.catma.ui.client.ui.tagger.editor.TaggerEditor;
import de.catma.ui.client.ui.tagger.editor.TaggerEditorListener;
import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;


/**
 * @author marco.petris@web.de
 *
 */
public class VTagger extends Composite {
	
	private static Logger logger = Logger.getLogger(VTagger.class.getName());
	
	private TaggerEditor taggerEditor;
	private ClientTagInstanceJSONSerializer tagInstanceJSONSerializer;
	private ClientTagDefinitionJSONSerializer tagDefinitionJSONSerializer;
	private TextRangeJSONSerializer textRangeJSONSerializer;

	private TaggerListener taggerListener;
	
	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VTagger() {
		this.tagInstanceJSONSerializer = new ClientTagInstanceJSONSerializer();
		this.tagDefinitionJSONSerializer = new ClientTagDefinitionJSONSerializer();
		this.textRangeJSONSerializer = new TextRangeJSONSerializer();
		initComponents();
		
	}
	
	private void initComponents() {

		taggerEditor = new TaggerEditor(new TaggerEditorListener() {
			public void tagChanged(TaggerEditorEventType type, Object... args) {
				switch(type) {
				
					case ADD : {
						ClientTagInstance tagInstance = (ClientTagInstance)args[0];
						taggerListener.tagInstanceAdded( 
								tagInstanceJSONSerializer.toJSONObject(tagInstance));
						break;
					}
				
					case REMOVE : {
						String tagInstanceID  = (String)args[0];
						boolean reportToServer = (Boolean)args[1];
						if (reportToServer) {
							taggerListener.tagInstanceRemoved(tagInstanceID);
						}
						break;
					}
					
				}
			}
			
			public void tagsSelected(List<String> tagInstanceIDs) {
				taggerListener.tagInstancesSelected(
						tagInstanceJSONSerializer.toJSONArray(tagInstanceIDs));
			}
			
			public void logEvent(String event) {
				logToServer(event);
			}
		});
		
		initWidget(taggerEditor);
	}
	
	public void logToServer(String logMsg) {
		taggerListener.log(logMsg);
	}

	public void setTaggerListener(TaggerListener taggerListener) {
		this.taggerListener = taggerListener;
	}

	public void setTaggerId(String taggerId) {
		taggerEditor.setTaggerID(taggerId);
	}

	public void setPage(String page) {
		logger.info("setting page content");
		taggerEditor.setHTML(new HTML(page));
	}

	public void removeTagInstances(String tagInstancesJson) {
		List<ClientTagInstance> tagInstances = 
				tagInstanceJSONSerializer.fromJSONArray(tagInstancesJson);
		
		for (ClientTagInstance tagInstance : tagInstances) {
			logger.info("got TagInstance from server (hide): " + tagInstance);
			taggerEditor.removeTagInstance(
					tagInstance.getInstanceID(), 
					false); //don't report to server
		}
	}

	public void highlight(String textRangeJson) {
		TextRange textRange = textRangeJSONSerializer.fromJSON(textRangeJson);
		taggerEditor.highlight(textRange);
	}

	public void addTagInstances(String tagInstancesJson) {
		List<ClientTagInstance> tagInstances = 
				tagInstanceJSONSerializer.fromJSONArray(tagInstancesJson);
		
		for (ClientTagInstance tagInstance : tagInstances) {
			logger.info("got tag instance from server (show): " + tagInstance);
			taggerEditor.addTagInstance(tagInstance);
		}
	}

	public void addTagInstanceWith(String tagDefinitionJson) {
		ClientTagDefinition tagDefinition = 
				tagDefinitionJSONSerializer.fromJSON(tagDefinitionJson);
			taggerEditor.createAndAddTagIntance(tagDefinition);
	}

}
