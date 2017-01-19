package de.catma.ui.analyzer;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.i18n.HelpTexts;

public class AnalyzerHelpWindow extends Window {
		
	public AnalyzerHelpWindow() {
		super(HelpTexts.getString("AnalyzerHelpWindow.title")); //$NON-NLS-1$
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
		
		Label helpText = new Label(HelpTexts.getString("AnalyzerHelpWindow.helpText"), ContentMode.HTML); //$NON-NLS-1$
		content.addComponent( helpText);
		setContent(content);
		
	}

	
}