package de.catma.ui.client.ui.zoomableverticallayout.impl;

import com.google.gwt.dom.client.Element;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomHandler;

public class IEImplZoomHandler implements ZoomHandler {
	public void zoom(Element element, double factor) {
		element.getStyle().setProperty("zoom", String.valueOf(factor));
		if (factor != 1.0) {
			element.getStyle().setProperty("zIndex", "1");
			Element.as(element.getChild(0)).getStyle().setProperty("zIndex", "1");
		}
		else {
			element.getStyle().setProperty("zIndex", "0");
			Element.as(element.getChild(0)).getStyle().setProperty("zIndex", "0");
		}
	};
}
