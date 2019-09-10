package de.catma.ui.module.analyze.visualization.vega;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class VegaHelpWindow extends Window {
		
	public VegaHelpWindow() {
		super("Help | Vega");
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
		
		Label helpText = new Label("<p>This URL represents the JSON formatted query results. \\\n" + 
				"You can either use this URL as the data URL or provide the data directly as values or use the placeholder CATMA_QUERY_URL which will be replaced with this URL.</p>\\\n" + 
				"<p>The JSON is an array of row objects. Each row provides at least the following fields: \\\n" + 
				"<ul>\\\n" + 
				"<li>sourceDocumentId - The identifier of the Source Document this row belongs to.</li> \\\n" + 
				"<li>phrase - The phrase of this row.</li> \\\n" + 
				"<li>startOffset - The start character offset of this row.</li> \\\n" + 
				"<li>endOffset - The end character offset of this row.</li> \\\n" + 
				"<li>sourceDocumentSize - The size of the document in number of characters.</li> \\\n" + 
				"<li>sourceDocumentTitle - The title of the Source Document.</li> \\\n" + 
				"</ul> \\\n" + 
				"If the query was a tag based query each row provides these additional fields: \\\n" + 
				"<ul>\\\n" + 
				"<li>annotationCollectionId - The identifier of the Annotation Collection.</li> \\\n" + 
				"<li>tagId - The identifier of the Tag.</li>\\\n" + 
				"<li>tagPath - The path that represents the full hierarchy of the Tag.</li> \\\n" + 
				"<li>tagVersion - The version identifier of the Tag.</li> \\\n" + 
				"<li>annotationId - The identifier of the Annotation</li> \\\n" + 
				"<li>propertyId - The identifier of the Property if the query was a Property based query, otherwise this field has a null value.</li> \\\n" + 
				"<li>propertyName - The name of the Property if the query was a Property based query, otherwise this field has a null value.</li> \\\n" + 
				"<li>propertyValue - The value of the Property if the query was a Property based query, otherwise this field has a null value.</li> \\\n" + 
				"</ul> \\\n" + 
				"</p>\\\n" + 
				"<p>If the annotation has more than one reference to the text, i. e. there are more than one startOffset - endOffset pair, a row for each text range is included.</p>\\\n" + 
				"For infos on how to construct a visalization specification see the <a href=\"https://vega.github.io/vega-lite/docs/\" target=\"_blank\">Vega-Lite documentation</a>. \\\n" + 
				"If you need more flexibility and power see the <a href=\"https://vega.github.io/vega/docs/\" target=\"_blank\">Vega documentation></a>.", 
				ContentMode.HTML);
		content.addComponent( helpText);
		setContent(content);
		
	}

	
}