package de.catma.ui.analyzer;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.repository.db.maintenance.UserManager;

public class AnalyzerHelpWindow extends Window {
		
	public AnalyzerHelpWindow() {
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
		
		Label helpText = new Label("<h4>Using the wordlist</h4>" +
				"Click on the  \"Wordlist\"-Button to get a list of all words of your document together with their frequencies." +
				" You can now sort the list by phrase, i. e. the word, or by frequency." +
				"<h4>Building queries</h4>" +
				"You are free to hack your query directly into the Query box, but a large part of all possible queries can be generated with the Query Builder more conveniently." +
				"<h4>Keywords in Context (KWIC)</h4>" +
				"To see your search results in the context of its surrounding text, tick the \"Visible in Kwic\"-check box " +
				"of the desired results." +
				"<h4>Results by Markup</h4>" +
				"When building Tag Queries where you look for occurrences of certain Tags, sometimes you " +
				"want the results grouped by Tags (especially Subtags) and sometimes you want the results " +
				"grouped by the tagged phrase. The \"Results by markup\" and \"Results by phrase\" tabs give you this choice for Tag Queries.", ContentMode.HTML);
		content.addComponent( helpText);
		setContent(content);
		
	}

	
}