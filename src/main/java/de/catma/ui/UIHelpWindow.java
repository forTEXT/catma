package de.catma.ui;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class UIHelpWindow extends Window {
		
	public UIHelpWindow() {
		super("Help | CATMA");  
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
		
		Label helpText = 
			new Label("<p>Watch out for these little question mark icons while navigating through CATMA. They provide useful hints for managing the first steps within a CATMA system.</p><h4>Sign up and login</h4><p>You can either login directly with a gmail address or you can sign up and create a CATMA account.</p><p>Once you're logged in, you will see the Project Manager, which will explain the first steps to you. If you ever get stuck, you can just click on the question mark icons!", ContentMode.HTML);
		helpText.addStyleName("help-text");
		content.addComponent( helpText);
		setContent(content);
		
	}

	
}