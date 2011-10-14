package de.catma.ui.tagger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Serializer;

import org.catma.document.Range;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;

import de.catma.ui.tagger.client.ui.VTagger;
import de.catma.ui.tagger.client.ui.shared.TaggedNode;
import de.catma.ui.tagger.client.ui.shared.TaggerEventAttribute;

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

		if (variables.containsKey(TaggerEventAttribute.TAGEVENT.name())) {

			System.out.println(variables.get(TaggerEventAttribute.TAGEVENT.name()));
			String event = (String)variables.get(TaggerEventAttribute.TAGEVENT.name());
			String[] eventParts = event.split("\\^");
			List<TaggedNode> taggedNodes = 
					TaggedNode.createTaggedNodes(Arrays.copyOfRange(eventParts, 1, eventParts.length));
			
			List<Range> ranges = htmlWrapper.addTag(eventParts[0], taggedNodes);
			
			Serializer serializer = new Serializer( System.out );
			serializer.setIndent( 4 );
			try {
				serializer.write(htmlWrapper.getHtmlDocModel());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setHTML(String html) {
		attributes.put(TaggerEventAttribute.HTML.name(), html);
		requestRepaint();
	}
	
	public void addTag(String tag) {
		attributes.put(TaggerEventAttribute.TAGEVENT.name(), tag);
		requestRepaint();				
	}

	public void setText(String text) {
		this.htmlWrapper = new HTMLWrapper(text);
		System.out.println(htmlWrapper);
		setHTML(htmlWrapper.toString());
	}
}
