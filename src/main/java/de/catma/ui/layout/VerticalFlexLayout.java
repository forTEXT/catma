package de.catma.ui.layout;

import com.vaadin.ui.Component;

public class VerticalFlexLayout extends FlexLayout {

	public VerticalFlexLayout(Component... components) {
		super(components);
		addStyle();
	}

	public VerticalFlexLayout(FlexDirection flexDirection, Component... components) {
		super(flexDirection, components);
		addStyle();
	}
	
	private void addStyle(){
		addStyleNames("flex-vertical");
	}
}
