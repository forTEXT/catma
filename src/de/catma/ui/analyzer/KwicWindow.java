package de.catma.ui.analyzer;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public class KwicWindow extends Window {
	
	public KwicWindow(String title, KwicPanel kwicPanel) {
		super(title);
		initComponents(kwicPanel);
	}

	private void initComponents(KwicPanel kwicPanel) {
		setContent(kwicPanel);
		center();
		setWidth("30%");
		setHeight("30%");
	}
	
	public void show() {
		UI.getCurrent().addWindow(this);
	}

}
