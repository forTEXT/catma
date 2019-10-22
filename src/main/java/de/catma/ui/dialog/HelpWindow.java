package de.catma.ui.dialog;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.ui.component.IconButton;

public class HelpWindow extends Window {
		
	public HelpWindow(String section, String helptext) {
		super("Help | " + section);  
		initComponents(helptext);
		setHeight("500px"); //$NON-NLS-1$
		setWidth("400px"); //$NON-NLS-1$
		center();
		setStyleName("help-windows"); //$NON-NLS-1$
	}

	private void initComponents(String helptext) {
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		
		Label helpText = 
			new Label(helptext, ContentMode.HTML);
		helpText.addStyleName("help-text");
		content.addComponent( helpText);
		setContent(content);
		
	}

	public Button createHelpWindowButton() {
		return new IconButton(VaadinIcons.QUESTION_CIRCLE, click -> {
			if (HelpWindow.this.getParent() == null) {
				UI.getCurrent().addWindow(HelpWindow.this);
			} else {
				UI.getCurrent().removeWindow(HelpWindow.this);
			}
		});	
	}
}