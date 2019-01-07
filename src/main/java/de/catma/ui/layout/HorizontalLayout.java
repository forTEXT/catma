package de.catma.ui.layout;

import com.vaadin.ui.Component;

public class HorizontalLayout extends FlexLayout{

	public HorizontalLayout(Component... components) {
		super(components);
		addStyle();
	}

	public HorizontalLayout(FlexDirection flexDirection, Component... components) {
		super(flexDirection, components);
		addStyle();
	}

	private void addStyle(){
		addStyleNames("flex-horizontal");
	}
	
}
