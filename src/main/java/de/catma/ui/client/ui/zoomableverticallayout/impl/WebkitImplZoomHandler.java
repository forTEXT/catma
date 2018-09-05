package de.catma.ui.client.ui.zoomableverticallayout.impl;

import com.google.gwt.dom.client.Element;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomHandler;

public class WebkitImplZoomHandler implements ZoomHandler {

	@Override
	public void zoom(Element element, double factor) {
		element.getStyle().setProperty("WebkitTransform", "scale("+factor+")");
		element.getStyle().setProperty("WebkitTransformOrigin", "left top 0");
		if (factor != 1.0) {
			element.getStyle().setProperty("zIndex", "1");
			Element.as(element.getChild(0)).getStyle().setProperty("zIndex", "1");
			element.getParentElement().getStyle().setProperty("zIndex", "1");
		}
		else {
			element.getStyle().setProperty("zIndex", "0");
			Element.as(element.getChild(0)).getStyle().setProperty("zIndex", "0");
			element.getParentElement().getStyle().setProperty("zIndex", "0");
		}
	}

}
