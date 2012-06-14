package de.catma.ui.client.ui.tagger.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

public class HighlightedSpanFactory extends SpanFactory {
	
	private String highlightColor;
	
	public HighlightedSpanFactory(String highlightColor) {
		this.highlightColor = highlightColor;
	}

	@Override
	public Element createTaggedSpan(String innerHtml) {
		Element highlightedSpan = DOM.createSpan();
		String style = 
				"display:inline-block; color:#F0F0F0; background:"
						+highlightColor+";";
		
		highlightedSpan.setAttribute("style", style);
		highlightedSpan.setId(getInstanceID() + "_" + instanceReferenceCounter++);
		highlightedSpan.setInnerHTML(innerHtml);
		return highlightedSpan;
	}

}
