package de.catma.ui.component;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

public class LinkButton extends Button{

	public LinkButton() {
		super();
		addStyleNames(ValoTheme.BUTTON_LINK);
	}

	public LinkButton(Resource icon, ClickListener listener) {
		super(icon, listener);
		addStyleNames(ValoTheme.BUTTON_LINK);
	}

	public LinkButton(Resource icon) {
		super(icon);
		addStyleNames(ValoTheme.BUTTON_LINK);
	}

	public LinkButton(String caption, ClickListener listener) {
		super(caption, listener);
		addStyleNames(ValoTheme.BUTTON_LINK);
	}

	public LinkButton(String caption, Resource icon) {
		super(caption, icon);
		addStyleNames(ValoTheme.BUTTON_LINK);
	}

	public LinkButton(String caption) {
		super(caption);
		addStyleNames(ValoTheme.BUTTON_LINK);
	}


	
}
