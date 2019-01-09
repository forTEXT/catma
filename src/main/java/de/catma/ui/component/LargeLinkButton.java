package de.catma.ui.component;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

public class LargeLinkButton extends Button{

	public LargeLinkButton() {
		super();
		init();
	}

	public LargeLinkButton(Resource icon, ClickListener listener) {
		super(icon, listener);
		init();
	}

	public LargeLinkButton(Resource icon) {
		super(icon);
		init();
	}

	public LargeLinkButton(String caption, ClickListener listener) {
		super(caption, listener);
		init();
	}

	public LargeLinkButton(String caption, Resource icon) {
		super(caption, icon);
		init();
	}

	public LargeLinkButton(String caption) {
		super(caption);
		init();
	}
	
	private void init() {
		addDefaultStyleNames();
	}

	private void addDefaultStyleNames(){
		addStyleNames(ValoTheme.LABEL_H3, ValoTheme.BUTTON_LINK);

	}

	
}
