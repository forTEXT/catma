package de.catma.ui.module.dashboard;


import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class GroupManagerHelpWindow extends Window {
		
	public GroupManagerHelpWindow() {
		super("Help | Groups"); 
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
		
		Label helpText = new Label("<h4>Working with Groups</h4><p>To start with your first CATMA user group just hit the 'Create User Group' card and enter a group name. Then click on the + sign to invite some group members.</p>", ContentMode.HTML);
		helpText.addStyleName("help-text");
		content.addComponent( helpText);
		setContent(content);
		
	}
}