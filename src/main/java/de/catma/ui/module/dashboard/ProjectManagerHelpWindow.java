package de.catma.ui.module.dashboard;


import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class ProjectManagerHelpWindow extends Window {
		
	public ProjectManagerHelpWindow() {
		super("Help | Projects"); 
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
		
		Label helpText = new Label("<h4>Working with Projects</h4><p>To start with your first CATMA Project just hit the 'Create Project' card and enter a Project name. Then click on the newly created Project card.</p>", ContentMode.HTML);
		helpText.addStyleName("help-text");
		content.addComponent( helpText);
		setContent(content);
		
	}
}