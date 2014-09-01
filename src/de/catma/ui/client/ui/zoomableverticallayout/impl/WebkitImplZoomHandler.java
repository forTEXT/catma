package de.catma.ui.client.ui.zoomableverticallayout.impl;

import com.google.gwt.dom.client.Element;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomHandler;

public class WebkitImplZoomHandler implements ZoomHandler {

	@Override
	public void zoom(Element element, double factor) {
		element.getStyle().setProperty("WebkitTransform", "scale("+factor+")");
	}

}
