package de.catma.ui.module.annotate;


import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class TaggerHelpWindow extends Window {
		
	public TaggerHelpWindow() {
		super("Help | Annotate"); 
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
		
		Label helpText = new Label("<h4>Annotate this source document</h4><ol><li>First you have to tell CATMA which tagset you want to use. Open a tag library from the Repository Manager and drag a tagset to the \"Active Tagsets\" section.</li><li>Now you can select the text sequence you want to tag.</li><li>Click the colored button of the desired tag to apply it to the selected sequence.</li></ol>When you click on an annotated text, i.e. a text that is underlined with colored bars, you should see the available annotation in the section on the lower right of this view.", ContentMode.HTML);
		content.addComponent( helpText);
		setContent(content);
		
	}
}