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
package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagDefinition;
import de.catma.core.util.ColorConverter;
import de.catma.ui.client.ui.tagger.VTagger;
import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TaggerMessageAttribute;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.tagger.pager.Page;
import de.catma.ui.tagger.pager.Pager;


/**
 * @author marco.petris@web.de
 *
 */
@ClientWidget(VTagger.class)
public class Tagger extends AbstractComponent {
	
	public static interface TaggerListener {
		public void tagInstanceAdded(ClientTagInstance clientTagInstance);
	}
	
	private static final long serialVersionUID = 1L;

	private Map<String,String> attributes = new HashMap<String, String>();
	private Pager pager;
	private TaggerListener taggerListener;
	private ClientTagInstanceJSONSerializer tagInstanceJSONSerializer;
	private boolean init = true;
	
	public Tagger(int taggerID, Pager pager, TaggerListener taggerListener) {
		addStyleName("tagger");
		this.pager = pager;
		this.taggerListener = taggerListener;
		this.tagInstanceJSONSerializer = new ClientTagInstanceJSONSerializer();
		attributes.put(TaggerMessageAttribute.ID.name(), String.valueOf(taggerID));
	}

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		
		if (target.isFullRepaint() 
				&& !pager.isEmpty() 
				&& !attributes.containsKey(TaggerMessageAttribute.PAGE_SET.name())) {
			
			attributes.put(
				TaggerMessageAttribute.PAGE_SET.name(), 
				pager.getCurrentPage().toHTML());
			
			try {
				attributes.put(
						TaggerMessageAttribute.TAGINSTANCES_ADD.name(),
						tagInstanceJSONSerializer.toJSON(
								pager.getCurrentPage().getRelativeTagInstances()));
			}
			catch(JSONSerializationException e) {
				//TODO: handle
				e.printStackTrace();
			}

		}

		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			target.addAttribute(entry.getKey(), entry.getValue());
		}
		
		attributes.clear();
	}

	/**
	 * Receive and handle events and other variable changes from the client.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		super.changeVariables(source, variables);

		if (variables.containsKey(TaggerMessageAttribute.TAGINSTANCE_ADD.name())) {
			try {
				ClientTagInstance tagInstance = tagInstanceJSONSerializer.fromJSON(
						(String)variables.get(TaggerMessageAttribute.TAGINSTANCE_ADD.name()));
				System.out.println("TagInstance added: " + tagInstance);
				pager.getCurrentPage().addRelativeTagInstance(tagInstance);
				taggerListener.tagInstanceAdded(
						pager.getCurrentPage().getAbsoluteTagInstance(tagInstance));
			} catch (JSONSerializationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (variables.containsKey(TaggerMessageAttribute.TAGINSTANCE_REMOVE.name())) {
			String instanceID = 
					(String)variables.get(
							TaggerMessageAttribute.TAGINSTANCE_REMOVE.name());
					System.out.println("TagInstance removed: " + instanceID);
			pager.getCurrentPage().removeRelativeTagInstance(instanceID);
		}
		
		if (variables.containsKey(TaggerMessageAttribute.LOGMESSAGE.name())) {
			System.out.println(
				"Got log message from client: "  
					+ variables.get(TaggerMessageAttribute.LOGMESSAGE.name()));
		}
	}
	
	private void setPage(String pageContent) {
		attributes.put(TaggerMessageAttribute.PAGE_SET.name(), pageContent);
		try {
			attributes.put(
					TaggerMessageAttribute.TAGINSTANCES_ADD.name(),
					tagInstanceJSONSerializer.toJSON(
							pager.getCurrentPage().getRelativeTagInstances()));
		} catch (JSONSerializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		requestRepaint();
	}

	public void setText(String text) {
		pager.setText(text);
		setPage(pager.getCurrentPage().toHTML());
	}
	
	public void setPage(int pageNumber) {
		Page page = pager.getPage(pageNumber);
		setPage(page.toHTML());
	}

	private void setTagInstancesVisible(
			List<ClientTagInstance> tagInstances, boolean visible) {
		
		List<ClientTagInstance> currentRelativePageTagInstancesCopy = 
				new ArrayList<ClientTagInstance>();
		
		currentRelativePageTagInstancesCopy.addAll(
				pager.getCurrentPage().getRelativeTagInstances());
		
		for (ClientTagInstance ti : tagInstances) {
			List<Page> pages = pager.getPagesForAbsoluteTagInstance(ti);
			if (!pages.isEmpty()) {
				if (visible) {
					for (Page page : pages) {
						page.addAbsoluteTagInstance(ti);
					}
				}
				else {
					for (Page page : pages) {
						page.removeRelativeTagInstance(ti.getInstanceID());
					}
				}
			}	
		}
		
		// we send only the TagInstances of the current page
		if (visible) {
			currentRelativePageTagInstancesCopy.clear();
			currentRelativePageTagInstancesCopy.addAll(
					pager.getCurrentPage().getRelativeTagInstances());
		}
		currentRelativePageTagInstancesCopy.retainAll(tagInstances);
		
		
		String taggerMessageAttribute = 
				TaggerMessageAttribute.TAGINSTANCES_ADD.name();
		if (!visible) {
			taggerMessageAttribute = 
				TaggerMessageAttribute.TAGINSTANCES_REMOVE.name();
		}
		
		try {
			attributes.put(
					taggerMessageAttribute,
					tagInstanceJSONSerializer.toJSON(
							currentRelativePageTagInstancesCopy));
		} catch (JSONSerializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		requestRepaint();
	}

	public void addTagInstanceWith(TagDefinition tagDefinition) {
		try {
			attributes.put(
				TaggerMessageAttribute.TAGDEFINITION_SELECTED.name(), 
				new ClientTagDefinitionJSONSerializer().toJSON(
						new ClientTagDefinition(
							tagDefinition.getID(),
							ColorConverter.toHex(tagDefinition.getColor()))));
		} catch (JSONSerializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		requestRepaint();
	}

	public void setVisible(List<TagReference> tagReferences, boolean visible) {
		List<ClientTagInstance> tagInstances = new ArrayList<ClientTagInstance>();
		
		for (TagReference tagReference : tagReferences) {
			System.out.println(
					"Setting TagReference: " + tagReference 
					+ " visible: " + visible);
			List<TextRange> textRanges = new ArrayList<TextRange>();
			textRanges.add(
					new TextRange(
							tagReference.getRange().getStartPoint(), 
							tagReference.getRange().getEndPoint()));
			
			tagInstances.add(
				new ClientTagInstance(
					tagReference.getTagDefinition().getID(),
					tagReference.getTagInstanceID(), 
					ColorConverter.toHex(tagReference.getColor()), 
					textRanges));
		}
		setTagInstancesVisible(tagInstances, visible);
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init) {
			WebApplicationContext context = 
					((WebApplicationContext) getApplication().getContext());
			WebBrowser wb = context.getBrowser();
			
			setHeight(wb.getScreenHeight()*0.47f, UNITS_PIXELS);
			init = false;
		}
	}
}
