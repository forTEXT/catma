package de.catma.ui.module.project;

import java.util.List;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class CollectionImportDialog extends AbstractOkCancelDialog<List<TagsetDefinitionImportStatus>> {

	private List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList;

	public CollectionImportDialog(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList,
			SaveCancelListener<List<TagsetDefinitionImportStatus>> saveCancelListener) {
		super("Import a Collection", saveCancelListener);
		this.tagsetDefinitionImportStatusList = tagsetDefinitionImportStatusList;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		Label intro = new Label(
				String.format(
						"We found %1$d Tagset(s) in the uploaded Collection.<br />"
						+ "Importing the Collection will also import or update the following Tagsets:", 
						tagsetDefinitionImportStatusList.size()), ContentMode.HTML);
		content.addComponent(intro);
		VerticalLayout tagsetPanel = new VerticalLayout();
		
		Panel tagsetScrollPanel = new Panel(tagsetPanel);
		tagsetScrollPanel.setSizeFull();
		content.addComponent(tagsetScrollPanel);
		((AbstractOrderedLayout)content).setExpandRatio(tagsetScrollPanel, 0.9f);
		
		for (TagsetDefinitionImportStatus tagsetDefinitionImportStatus : this.tagsetDefinitionImportStatusList) {
			HorizontalLayout row = new HorizontalLayout();
			row.setSpacing(true);
			tagsetPanel.addComponent(row);
			
			Label rowLabel = new Label("", ContentMode.HTML);
			
			if (!tagsetDefinitionImportStatus.isCurrent()) {
				
				String doImportText = 
						String.format(
								"<b>%1$s</b> is new to this Project and will be imported!", 
								tagsetDefinitionImportStatus.getTagset().getName()); 

				rowLabel.setValue(doImportText);
			}
			else {
				String doImportText = 
						String.format("<b>%1$s</b> is already part of this Project!<br />"
						+ "Importing the Collection will merge not yet included Tags, Properties "
						+ "and Values<br />from the Tagset of the imported Collection into the existing Tagset.<br />" 
						+ "%1$s will be imported!", 
						tagsetDefinitionImportStatus.getTagset().getName());
				rowLabel.setValue(doImportText);
			}
			row.addComponent(rowLabel);
			row.setExpandRatio(rowLabel, 1.0f);
		}
	
	}

	@Override
	protected List<TagsetDefinitionImportStatus> getResult() {
		return tagsetDefinitionImportStatusList;
	}

	@Override
	protected String getOkCaption() {
		return "Import";
	}
}
