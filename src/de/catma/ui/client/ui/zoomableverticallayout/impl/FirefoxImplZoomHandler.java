package de.catma.ui.client.ui.zoomableverticallayout.impl;

import com.google.gwt.dom.client.Element;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomHandler;

public class FirefoxImplZoomHandler implements ZoomHandler {
	public void zoom(Element element, double factor) {
		element.getStyle().setProperty("MozTransform", "scale("+factor+")");
	};
}
