package de.catma.ui.client.ui.zoomableverticallayout.impl;

import com.google.gwt.dom.client.Element;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomHandler;

public class FirefoxImplZoomHandler implements ZoomHandler {
	public void zoom(Element element, double factor) {
		element.getStyle().setProperty("MozTransform", "scale("+factor+")");
		element.getStyle().setProperty("MozTransformOrigin", "left top 0");
		element.getStyle().setProperty("zoom", String.valueOf(factor)); //IE11 claims to be Gecko
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
