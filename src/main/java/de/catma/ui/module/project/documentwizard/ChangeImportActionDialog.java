package de.catma.ui.module.project.documentwizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Notification.Type;

import de.catma.project.Project;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.IDGenerator;

public class ChangeImportActionDialog extends AbstractOkCancelDialog<TagsetDefinition> {
	
	private List<TagsetDefinition> tagsets;
	private Grid<TagsetDefinition> tagsetGrid;
	private RadioButtonGroup<String> choices;
	private TextField tagsetNameInput;
	private String choice1;
	private String choice2;
	private String choice3;

	public ChangeImportActionDialog(
		Project project, Collection<TagsetDefinition> importTagsets, 
		SaveCancelListener<TagsetDefinition> saveCancelListener) throws Exception {
		super("Change Tagset Import Action", saveCancelListener);
		
		
		tagsets = new ArrayList<>(project.getTagManager().getTagLibrary().getTagsetDefinitions());
		
		for (TagsetDefinition importTagset : importTagsets) {
			if (!tagsets.contains(importTagset)) {
				tagsets.add(importTagset);
			}
		}
		
		Collections.sort(tagsets, (t1, t2) -> {
			if (t1.getName().equals(t2.getName())) {
				return t1.getUuid().compareTo(t2.getUuid());
			}
			
			return t1.getName().compareTo(t2.getName());
		});
		
		this.choice1 = "to create a new Tagset";
		this.choice2 = "to select a different Tagset";
		this.choice3 = "or to ignore the Tags";

	}
	
	
	@Override
	protected void addContent(ComponentContainer content) {
		HorizontalLayout choicePanel = new HorizontalLayout();
		ListDataProvider<TagsetDefinition> tagsetGridDataProvider = 
				new ListDataProvider<TagsetDefinition>(tagsets);
		this.tagsetGrid = new Grid<>("Available Tagsets", tagsetGridDataProvider);
		this.tagsetGrid.setHeight("200px");
		tagsetGrid.addColumn(tagset -> tagset.getName())
		.setCaption("Name");
		
		this.tagsetNameInput = new TextField("Enter the name of the new Tagset");
	
		this.choices = new RadioButtonGroup<String>("Your choices are");
		
		
		
		this.choices.setItems(choice1, choice2, choice3);
		this.choices.addValueChangeListener(event -> {
			choicePanel.removeAllComponents();
			if (event.getValue().equals(choice1)) {
				choicePanel.addComponent(this.tagsetNameInput);
			}
			else if (event.getValue().equals(choice2)) {
				choicePanel.addComponent(this.tagsetGrid);
			}
		});
		
		content.addComponent(this.choices);
		content.addComponent(choicePanel);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout) content).setComponentAlignment(choices, Alignment.TOP_CENTER);
			((AbstractOrderedLayout) content).setComponentAlignment(choicePanel, Alignment.TOP_CENTER);
		}
		
		this.choices.setValue(choice2);
	}
	
	@Override
	protected void handleOkPressed() {
		if (choices.getValue().equals(choice1)) {
			String name = tagsetNameInput.getValue();
			if ((name == null) || name.isEmpty()) {
				Notification.show("Info", "Please enter a name!", Type.HUMANIZED_MESSAGE);
				return;
			}
		}
		else if (choices.getValue().equals(choice2) && tagsetGrid.getSelectedItems().isEmpty()){
			Notification.show("Info", "Please select one Tagset!", Type.HUMANIZED_MESSAGE);
			return;
		}	
		
		super.handleOkPressed();
	}

	@Override
	protected TagsetDefinition getResult() {
		if (choices.getValue().equals(choice1)) {
			String name = tagsetNameInput.getValue();
			IDGenerator idGenerator = new IDGenerator();
			return new TagsetDefinition(idGenerator.generateTagsetId(), name, new Version());
		}
		else if (choices.getValue().equals(choice2)) {
			return this.tagsetGrid.getSelectedItems().iterator().next();
		}
		
		return null;
	}

}
