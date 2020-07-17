package de.catma.ui.client.ui.component;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ButtonBase;

public abstract class IconButton extends ButtonBase {

	protected IconButton(String style, String iconName, int iconHexCode) {
		super(DOM.createDiv());
		addStyleName(style);
		addStyleName("v-button");
		addStyleName("v-widget");
		addStyleName("icon-only");
		addStyleName("v-button-icon-only");
		addStyleName("button__icon");
		addStyleName("v-button-button__icon");
		addStyleName("flat v-button-flat");
		addStyleName("borderless");
		addStyleName("v-button-borderless");
		
		setHTML(
			"<span class=\"v-icon v-icon-" + iconName
                + "\" style=\"font-family: Vaadin-Icons;\">&#x"
                + Integer.toHexString(iconHexCode) + ";</span>");
	}

	@Override
	public void setVisible(boolean visible) {
		getElement().getStyle().setVisibility(visible?Visibility.VISIBLE:Visibility.HIDDEN);
	}
	
}
