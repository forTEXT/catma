package de.catma.ui.visualizer.vega;

import com.vaadin.ui.AbstractComponent;

import de.catma.ui.client.ui.visualizer.vega.VegaState;

public class Vega extends AbstractComponent {
	
	public Vega() {
	}
	
	public void setVegaSpec(String vegaSpec) {
		this.getState().vegaSpec = vegaSpec;
	}

	
	@Override
	protected VegaState getState() {
		return (VegaState)super.getState();
	}
}
