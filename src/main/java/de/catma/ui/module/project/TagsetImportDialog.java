package de.catma.ui.module.project;

import java.util.List;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.util.HtmlEscaper;

public class TagsetImportDialog extends AbstractOkCancelDialog<List<TagsetDefinitionImportStatus>> {

	private List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList;

	public TagsetImportDialog(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList,
			SaveCancelListener<List<TagsetDefinitionImportStatus>> saveCancelListener) {
		super("Import Tagsets", saveCancelListener);
		this.tagsetDefinitionImportStatusList = tagsetDefinitionImportStatusList;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		Label intro = new Label(
				String.format(
						"We found %1$d tagset(s) in the uploaded tag library:",
						tagsetDefinitionImportStatusList.size()));
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
			
			CheckBox cbImport = new CheckBox(null, true);
			
			if (!tagsetDefinitionImportStatus.isCurrent()) {
				
				String doImportText = String.format(
						"<strong>%s</strong> will be imported!",
						HtmlEscaper.escape(tagsetDefinitionImportStatus.getTagset().getName())
				);
				String doNotImportText = String.format(
						"<strong>%s</strong> will <strong>NOT</strong> be imported!",
						HtmlEscaper.escape(tagsetDefinitionImportStatus.getTagset().getName())
				);
				rowLabel.setValue(doImportText);
				
				cbImport.addValueChangeListener(event -> {
					if (event.getValue()) {
						rowLabel.setValue(doImportText);
					}  
					else {
						rowLabel.setValue(doNotImportText);
					}
				});
			}
			else {
				String doImportText = String.format(
						"<strong>%1$s</strong> is already part of this project!<br />"
						+ "Importing it will merge new tags, properties and values from the imported tagset into the existing tagset.<br />"
						+ "Please tick off the check box to import this tagset!<br />"
						+ "%1$s will be imported!",
						HtmlEscaper.escape(tagsetDefinitionImportStatus.getTagset().getName())
				);
				String doNotImportText = String.format(
						"<strong>%1$s</strong> is already part of this project!<br />"
						+ "Importing it will merge new tags, properties and values from the imported tagset into the existing tagset.<br />"
						+ "Please tick off the check box to import this tagset!<br />"
						+ "%1$s will <strong>NOT</strong> be imported!",
						HtmlEscaper.escape(tagsetDefinitionImportStatus.getTagset().getName())
				);

				rowLabel.setValue(doNotImportText);
				cbImport.setValue(false);
				tagsetDefinitionImportStatus.setDoImport(false);
				
				cbImport.addValueChangeListener(event -> {
					if (event.getValue()) {
						rowLabel.setValue(doImportText);
					}  
					else {
						rowLabel.setValue(doNotImportText);
					}
				});				

			}
			row.addComponent(rowLabel);
			row.addComponent(cbImport);
			row.setComponentAlignment(cbImport, Alignment.MIDDLE_RIGHT);
			row.setExpandRatio(rowLabel, 1.0f);
			
			cbImport.addValueChangeListener(
					event -> tagsetDefinitionImportStatus.setDoImport(event.getValue()));
			
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
