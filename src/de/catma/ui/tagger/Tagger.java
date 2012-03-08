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
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagDefinition;
import de.catma.core.util.ColorConverter;
import de.catma.ui.client.ui.tagger.VTagger;
import de.catma.ui.client.ui.tagger.shared.EventAttribute;
import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.tagger.TagInstanceJSONSerializer.JSONSerializationException;
import de.catma.ui.tagger.pager.Page;
import de.catma.ui.tagger.pager.Pager;


/**
 * @author marco.petris@web.de
 *
 */
@ClientWidget(VTagger.class)
public class Tagger extends AbstractComponent {
	
	public static interface TaggerListener {
		public void tagInstanceAdded(TagInstance tagInstance);
	}
	
	private static final long serialVersionUID = 1L;

	private Map<String,String> attributes = new HashMap<String, String>();
	private Pager pager;
	private TaggerListener taggerListener;
	private TagInstanceJSONSerializer tagInstanceJSONSerializer;
	
	public Tagger(Pager pager, TaggerListener taggerListener) {
		addStyleName("tagger");
		this.pager = pager;
		this.taggerListener = taggerListener;	
		this.tagInstanceJSONSerializer = new TagInstanceJSONSerializer();
	}

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		
		System.out.println(target + " fr:" + target.isFullRepaint());
		
		if (target.isFullRepaint() 
				&& !pager.isEmpty() 
				&& !attributes.containsKey(EventAttribute.PAGE_SET.name())) {
			
			attributes.put(
				EventAttribute.PAGE_SET.name(), 
				pager.getCurrentPage().toHTML());
			
			try {
				attributes.put(
						EventAttribute.TAGINSTANCES_ADD.name(),
						tagInstanceJSONSerializer.toJSON(
								pager.getCurrentPage().getTagInstances()));
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
		
		// We could also set variables in which values can be returned
		// but declaring variables here is not required
	}

	/**
	 * Receive and handle events and other variable changes from the client.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		super.changeVariables(source, variables);

		if (variables.containsKey(EventAttribute.TAGINSTANCE_ADD.name())) {
			try {
				TagInstance tagInstance = tagInstanceJSONSerializer.fromJSON(
						(String)variables.get(EventAttribute.TAGINSTANCE_ADD.name()));
				pager.getCurrentPage().addTagInstance(tagInstance);
				taggerListener.tagInstanceAdded(
						pager.getCurrentPage().getAbsoluteTagInstance(tagInstance));
			} catch (JSONSerializationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (variables.containsKey(EventAttribute.TAGINSTANCE_REMOVE.name())) {
			pager.getCurrentPage().removeTagInstance(
					(String)variables.get(EventAttribute.TAGINSTANCE_REMOVE.name()));
		}
		
		if (variables.containsKey(EventAttribute.LOGMESSAGE.name())) {
			System.out.println(variables.get(EventAttribute.LOGMESSAGE.name()));
		}
	}
	
	private void setPage(String pageContent) {
		attributes.put(EventAttribute.PAGE_SET.name(), pageContent);
		try {
			attributes.put(
					EventAttribute.TAGINSTANCES_ADD.name(),
					tagInstanceJSONSerializer.toJSON(
							pager.getCurrentPage().getTagInstances()));
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

	public void setTagInstances(List<TagInstance> tagInstances) {
		try {
			attributes.put(
					EventAttribute.TAGINSTANCES_ADD.name(),
					tagInstanceJSONSerializer.toJSON(
							tagInstances));
		} catch (JSONSerializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (TagInstance ti : tagInstances) {
			Page page = pager.getPageFor(ti);
			if (page != null) {
				page.addAbsoluteTagInstance(ti);
			}
		}
		requestRepaint();
	}

	public void addTagInstanceWith(TagDefinition tagDefinition) {
		attributes.put(
			EventAttribute.TAGDEFINITION_SELECTED.name(), 
			new ColorConverter(tagDefinition.getColor()).toHex());
		requestRepaint();
	}

	public void setVisible(List<TagReference> tagReferences, boolean visible) {
		List<TagInstance> tagInstances = new ArrayList<TagInstance>();
		
		for (TagReference tagReference : tagReferences) {
			List<TextRange> textRanges = new ArrayList<TextRange>();
			textRanges.add(
					new TextRange(
							tagReference.getRange().getStartPoint(), 
							tagReference.getRange().getEndPoint()));
			
			tagInstances.add(
				new TagInstance(
					tagReference.getTagInstanceID(), 
					new ColorConverter(tagReference.getColor()).toHex(), 
					textRanges));
		}
		if (visible) {
			setTagInstances(tagInstances);
		}
	}
}
