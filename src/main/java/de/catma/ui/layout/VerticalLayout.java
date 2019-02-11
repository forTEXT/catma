package de.catma.ui.layout;

import com.vaadin.ui.Component;

public class VerticalLayout extends FlexLayout {

	public VerticalLayout(Component... components) {
		super(components);
		addStyle();
	}

	public VerticalLayout(FlexDirection flexDirection, Component... components) {
		super(flexDirection, components);
		addStyle();
	}
	
	private void addStyle(){
		addStyleNames("flex-vertical");
	}
}
