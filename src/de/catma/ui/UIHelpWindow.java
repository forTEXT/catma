package de.catma.ui;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.repository.db.maintenance.UserManager;

public class UIHelpWindow extends Window {
		
	public UIHelpWindow() {
		super("Help");
		initComponents();
		setHeight("500px");
		setWidth("400px");
		center();
		setStyleName("help-windows");
	}

	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		
		Label helpText = new Label("<p>Watch out for these little question mark icons while navigating " +
					"through CATMA. They provide useful hints for managing the first " +
					"steps within a CATMA component.</p>" +
					"<h4>Login</h4>" +
					"Once you're logged in, you will see the Repository Manager, " +
					"which will explain the first steps to you. " +
					"Just hover your mouse over the question mark icons!", ContentMode.HTML);
		content.addComponent( helpText);
		setContent(content);
		
	}

	
}