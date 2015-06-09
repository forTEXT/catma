package de.catma.ui.analyzer;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.ui.tagmanager.TagManagerView;

public class TagManagerWindow extends Window {
	
	public TagManagerWindow(String caption, TagManagerView tagManagerView) {
		super(caption, tagManagerView);
		
		setWidth("35%");
		setHeight("85%");
	}
	
	public void show() {		
		UI.getCurrent().addWindow(this);
	}
}
