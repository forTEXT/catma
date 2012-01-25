package de.catma.ui.tagmanager;

import com.vaadin.ui.Window;

public class TagManagerWindow extends Window {

	public TagManagerWindow(TagManagerView tagManagerView) {
		super("Tag Manager");
		this.setContent(tagManagerView);
		setWidth("20%");
		setHeight("80%");
	}
}
