package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;

import de.catma.ui.client.ui.tagger.VTagger;
import de.catma.ui.client.ui.tagger.shared.EventAttribute;
import de.catma.ui.client.ui.tagger.shared.TagInstance;

/**
 * Server side component for the VMyComponent widget.
 */
@com.vaadin.ui.ClientWidget(VTagger.class)
public class Tagger extends AbstractComponent {
	private static final long serialVersionUID = 1L;

	private Map<String,String> attributes = new HashMap<String, String>();
	private List<String> tagInstances = new ArrayList<String>();
	private String html;
	
	private HTMLWrapper htmlWrapper;

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);

		if (target.isFullRepaint()) {
			attributes.put(EventAttribute.HTML.name(), html);
			target.addAttribute(EventAttribute.ALLTAGINSTANCES.name(), tagInstances.toArray(new String[] {}));
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

		// Variables set by the widget are returned in the "variables" map.

		if (variables.containsKey(EventAttribute.TAGINSTANCE.name())) {
			@SuppressWarnings("unchecked")
			TagInstance event = 
				new TagInstance(
						(Map<String,Object>)variables.get(EventAttribute.TAGINSTANCE.name()));
			tagInstances.add(event.toString());
			System.out.println(event);
			
		}
		
		if (variables.containsKey(EventAttribute.LOGMESSAGE.name())) {
			System.out.println(variables.get(EventAttribute.LOGMESSAGE.name()));
		}
	}
	
	private void setHTML(String html) {
		this.html = html;
		attributes.put(EventAttribute.HTML.name(), html);
		requestRepaint();
	}
	
	public void addTag(String tag) {
		attributes.put(EventAttribute.TAGINSTANCE.name(), tag);
		requestRepaint();				
	}

	public void setText(String text) {
		this.htmlWrapper = new HTMLWrapper(text);
		//htmlWrapper.print();
		setHTML(htmlWrapper.toString());
	}
}
