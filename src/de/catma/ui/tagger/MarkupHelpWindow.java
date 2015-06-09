package de.catma.ui.tagger;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.repository.db.maintenance.UserManager;

public class MarkupHelpWindow extends Window {
		
	public MarkupHelpWindow() {
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
		
		Label helpText = new Label("<h4>Creating Tags</h4>" +
				"<ol><li>First you have to tell CATMA which Tagset you want to use. " +
				"Open a Tag Type Library from the Repository Manager and drag a Tagset to the \"Active Tagsets\" section." +
				" If you already have an active Tagset you want to use, you can skip this step.</li>" +
				"<li>Now you can select the Tagset and click the \"Create Tag Type Type\"-Button.</li></ol>"+
				"<h4>Tag this Source Document</h4>" +
				"<ol><li>First you have to tell CATMA which Tagset you want to use. " +
				"Open a Tag Type Library from the Repository Manager and drag a Tagset to the \"Active Tagsets\" section." +
				" If you already have an active Tagset you want to use, you can skip this step.</li>" +
				"<li>Now you can mark the text sequence you want to tag.</li><li>Click the colored button of the desired Tag Type to apply it to the marked sequence.</li></ol> " +
				"When you click on a tagged text, i. e. a text that is underlined with colored bars you should see " +
				"the available Tag in the section on the lower right of this view.", ContentMode.HTML);
		content.addComponent( helpText);
		setContent(content);
		
	}
}