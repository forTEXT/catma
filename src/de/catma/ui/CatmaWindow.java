package de.catma.ui;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Window;

public class CatmaWindow extends Window {
	
	public CatmaWindow() {
		super();
	}

	public CatmaWindow(String caption, ComponentContainer content) {
		super(caption, content);
	}

	public CatmaWindow(String caption) {
		super(caption);
	}

	public void setPosition() {
		center();
	}
}
