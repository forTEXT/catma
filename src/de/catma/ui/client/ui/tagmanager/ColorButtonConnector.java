package de.catma.ui.client.ui.tagmanager;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.ui.button.ButtonConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.tagmanager.ColorButton;

@Connect(ColorButton.class)
public class ColorButtonConnector extends ButtonConnector {
	
	@Override
	protected VColorButton createWidget() {
		return GWT.create(VColorButton.class);
	}
	
	@Override
	public ColorButtonState getState() {
		return (ColorButtonState)super.getState();
	}
	
	 @OnStateChange("color")
	 void updateColor() {
		 getWidget().setColor(getState().color);
	 }
	 
	 @Override
	public VColorButton getWidget() {
		return (VColorButton) super.getWidget();
	}
}
