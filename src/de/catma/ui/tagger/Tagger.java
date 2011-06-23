package de.catma.ui.tagger;

import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;

import de.catma.ui.tagger.client.ui.TagEvent;
import de.catma.ui.tagger.client.ui.VTagger;

/**
 * Server side component for the VMyComponent widget.
 */
@com.vaadin.ui.ClientWidget(VTagger.class)
public class Tagger extends AbstractComponent {
	
	public enum Attribute {
		HTML,
		TAGEVENT;
	}
	
//	public static final String INIT_HTML = "nase<div id=\"bla0\">bla0" +
//	"    <div id=\"bla1\">bla1 bla1 bla1</div>" +
//	"    <div id=\"bla2\">bla2 bla2 bla2" +
//	"        <div id=\"bla2.1\">bla2.1 bla2.1 bla2.1</div>" +
//	"        <div id=\"bla2.2\">bla2.2 bla2.2 bla2.2</div>" +
//	"        <div id=\"bla2.3\">bla2.3 bla2.3 bla2.3</div>" +
//	"    </div>" +
//	"    <div id=\"bla3\">bla3 bla3 bla3</div>" +
//	"</div>";
	
	public static final String INIT_HTML = "not loaded";
	private String html = INIT_HTML;
	private String html_arg = INIT_HTML;
	private String tag = "";
	

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);

		// Paint any component specific content by setting attributes
		// These attributes can be read in updateFromUIDL in the widget.
		target.addAttribute(Attribute.TAGEVENT.name(), tag);
		target.addAttribute(Attribute.HTML.name(), html_arg);
		tag = "";
		html_arg = "";
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

		if (variables.containsKey("TAGEVENT")) {

			System.out.println(variables.get("TAGEVENT"));
			
//			requestRepaint();
		}
	}
	
	public void setHTML(String html) {
		this.html = html;
		this.html_arg = html;
		requestRepaint();
	}
	
	public void addTag(String tag) {
		this.tag = tag;
		requestRepaint();				
	}

}
