package de.catma.ui.client.ui.visualizer.vega;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.visualizer.vega.Vega;

@Connect(Vega.class)
public class VegaConnector extends AbstractComponentConnector {
	
	@Override
	protected Widget createWidget() {
		return GWT.create(VegaWidget.class);
	}

	@Override
	public VegaWidget getWidget() {
		return (VegaWidget) super.getWidget();
	}

	@Override
	public VegaState getState() {
		return (VegaState)super.getState();
	}
	
	@OnStateChange("vegaSpec")
	private void setVegaSpec() {
		getWidget().setVegaSpec(getState().vegaSpec);
	}
}
