package de.catma.ui.client.ui.zoomableverticallayout;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ui.orderedlayout.VerticalLayoutConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.component.ZoomableVerticalLayout;

@Connect(ZoomableVerticalLayout.class)
public class ZoomableVerticalLayoutConnector extends VerticalLayoutConnector {

	public ZoomableVerticalLayoutConnector() {
		registerRpc(ZoomableClientRpc.class, new ZoomableClientRpc() {
			
			@Override
			public void zoom(double factor) {
				getWidget().zoom(factor);
			}
		});
	}
	
	@Override
	protected VZoomableVerticalLayout createWidget() {
		return GWT.create(VZoomableVerticalLayout.class);
	}
	
	@Override
	public VZoomableVerticalLayout getWidget() {
		return (VZoomableVerticalLayout) super.getWidget();
	}
}
