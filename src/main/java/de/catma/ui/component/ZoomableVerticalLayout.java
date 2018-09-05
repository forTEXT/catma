package de.catma.ui.component;

import com.vaadin.v7.ui.VerticalLayout;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomableClientRpc;

public class ZoomableVerticalLayout extends VerticalLayout {

	public void zoom(double factor) {
		getRpcProxy(ZoomableClientRpc.class).zoom(factor);
	}
}
