package de.catma.ui.repository;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.repository.db.maintenance.UserManager;

public class RepositoryHelpWindow extends Window {
		
	public RepositoryHelpWindow() {
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
		
		Label helpText = new Label("<h4>First steps</h4>" +
				"<h5>Adding a Source Document</h5>" +
				"You can add a Source Document by clicking the \"Add Source Document\"-button. " +
				"A Source Document can be a web resource pointed to by the URL or you can upload a document from your computer. " +
				"<h5>Tagging a Source Document</h5>" +
				"When you add your first Source Document, CATMA generates a set of example items to get you going: " +
				"<ul><li>A Markup Collection to hold your markup</li><li>A Tag Type Library with an example Tagset that contains an example Tag</li></ul> "+
				"To start tagging a Source Document, just select the example Markup Collection from the tree and click the \"Open Markup Collection\"-button. " +
				"Then follow the instructions given to you by the Tagger component." +
				"<h5>Analyze a Source Document</h5>" +
				"To analyze a Source Document, just select that document from the tree and click \"Analyze Source Document\" in the \"More Actions\"-menu." +
				"Then follow the instructions given to you by the Analyzer component.", ContentMode.HTML);
		content.addComponent( helpText);
		setContent(content);
		
	}
}