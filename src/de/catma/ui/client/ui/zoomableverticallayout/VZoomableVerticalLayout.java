package de.catma.ui.client.ui.zoomableverticallayout;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.ui.VVerticalLayout;

import de.catma.ui.client.ui.zoomableverticallayout.impl.FirefoxImplZoomHandler;

public class VZoomableVerticalLayout extends VVerticalLayout {

	private static ZoomHandler zoomHandlerimpl = 
			GWT.create(FirefoxImplZoomHandler.class);  
	
	public VZoomableVerticalLayout() {
	}
	
	public void zoom(double factor) {
		zoomHandlerimpl.zoom(getElement(), factor);
	}

}
