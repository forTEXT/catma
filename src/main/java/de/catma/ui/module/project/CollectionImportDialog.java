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
import de.catma.ui.util.HtmlEscaper;

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
						"We found %1$d tagset(s) in the uploaded collection.<br />"
						+ "Importing the collection will also import or update the following tagsets:",
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
				String doImportText = String.format(
						"<strong>%s</strong> is new to this project and will be imported!",
						HtmlEscaper.escape(tagsetDefinitionImportStatus.getTagset().getName())
				);

				rowLabel.setValue(doImportText);
			}
			else {
				String doImportText = String.format(
						"<strong>%1$s</strong> is already part of this project!<br />"
						+ "Importing the collection will merge not yet included tags, properties "
						+ "and values<br />from the tagset of the imported collection into the existing tagset.<br />"
						+ "%1$s will be imported!",
						HtmlEscaper.escape(tagsetDefinitionImportStatus.getTagset().getName())
				);
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
