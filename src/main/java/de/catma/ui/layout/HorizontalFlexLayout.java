package de.catma.ui.layout;

import com.vaadin.ui.Component;

public class HorizontalFlexLayout extends FlexLayout{

	public HorizontalFlexLayout(Component... components) {
		super(components);
		addStyle();
	}

	public HorizontalFlexLayout(FlexDirection flexDirection, Component... components) {
		super(flexDirection, components);
		addStyle();
	}

	private void addStyle(){
		addStyleNames("flex-horizontal");
	}
	
}
