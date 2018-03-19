package de.catma.ui;

import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class UIHelpWindow extends Window {
		
	public UIHelpWindow() {
		super(Messages.getString("UIHelpWindow.helpTitle"));  //$NON-NLS-1$
		initComponents();
		setHeight("500px"); //$NON-NLS-1$
		setWidth("400px"); //$NON-NLS-1$
		center();
		setStyleName("help-windows"); //$NON-NLS-1$
	}

	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		
		Label helpText = new Label(Messages.getString("UIHelpWindow.helpText"), ContentMode.HTML); //$NON-NLS-1$
		content.addComponent( helpText);
		setContent(content);
		
	}

	
}