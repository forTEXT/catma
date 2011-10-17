package de.catma.ui.tagger;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;

import de.catma.ui.tagger.client.ui.VTagger;
import de.catma.ui.tagger.client.ui.shared.EventAttribute;
import de.catma.ui.tagger.client.ui.shared.TagEvent;

/**
 * Server side component for the VMyComponent widget.
 */
@com.vaadin.ui.ClientWidget(VTagger.class)
public class Tagger extends AbstractComponent {
	private static final long serialVersionUID = 1L;

	private Map<String,String> attributes = new HashMap<String, String>();

	private HTMLWrapper htmlWrapper;

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);

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

		if (variables.containsKey(EventAttribute.TAGEVENT.name())) {

			System.out.println(variables.get(EventAttribute.TAGEVENT.name()));
			@SuppressWarnings("unchecked")
			TagEvent event = new TagEvent((Map<String,Object>)variables.get(EventAttribute.TAGEVENT.name()));
			
			System.out.println(event);
			
		}
		
		if (variables.containsKey(EventAttribute.LOGEVENT.name())) {
			System.out.println(variables.get(EventAttribute.LOGEVENT.name()));
		}
	}
	
	private void setHTML(String html) {
		attributes.put(EventAttribute.HTML.name(), html);
		requestRepaint();
	}
	
	public void addTag(String tag) {
		attributes.put(EventAttribute.TAGEVENT.name(), tag);
		requestRepaint();				
	}

	public void setText(String text) {
		this.htmlWrapper = new HTMLWrapper(text);
		//htmlWrapper.print();
		setHTML(htmlWrapper.toString());
	}
}
