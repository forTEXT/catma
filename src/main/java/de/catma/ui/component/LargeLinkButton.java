package de.catma.ui.component;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

public class LargeLinkButton extends Button{

	public LargeLinkButton() {
		super();
		setStylenames();
	}

	public LargeLinkButton(Resource icon, ClickListener listener) {
		super(icon, listener);
		setStylenames();
	}

	public LargeLinkButton(Resource icon) {
		super(icon);
		setStylenames();
	}

	public LargeLinkButton(String caption, ClickListener listener) {
		super(caption, listener);
		setStylenames();
	}

	public LargeLinkButton(String caption, Resource icon) {
		super(caption, icon);
		setStylenames();
	}

	public LargeLinkButton(String caption) {
		super(caption);
		setStylenames();
	}

	private void setStylenames(){
		addStyleNames(ValoTheme.LABEL_H3, ValoTheme.BUTTON_LINK);

	}

	
}
