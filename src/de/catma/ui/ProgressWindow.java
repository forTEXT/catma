package de.catma.ui;

import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;

public class ProgressWindow extends CatmaWindow {

	public ProgressWindow(ProgressIndicator pi) {
		VerticalLayout layout = (VerticalLayout) getContent();
		layout.addComponent(pi);
		setClosable(false);
		addStyleName("progress_window");
		setDraggable(false);
		setWidth("300px");
		setHeight("100px");
		setStayOnTop(true);
	}
	
	@Override
	public void attach() {
		super.attach();
		setPositionX(0);
		setPositionY((int)(getParent().getHeight()-1.5*getHeight()));
	}
}
