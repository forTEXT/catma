package de.catma.ui.module.tags;

import com.vaadin.shared.ui.colorpicker.Color;

import java.util.Collection;

import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.ui.dialog.SaveCancelListener;

public class EditTagDialog extends AbstractAddEditTagDialog<TagDefinition> {
	
	private TagDefinition tagDefinition;

	public EditTagDialog(Collection<TagsetDefinition> scope, TagLibrary tagLibrary, TagDefinition tagDefinition, SaveCancelListener<TagDefinition> saveCancelListener) {
		super("Edit Tag", saveCancelListener);
		this.tagDefinition = tagDefinition;
		initComponents(scope, tagLibrary, tagDefinition, true);
		initActions();
		setPropertyDefinitionsVisible();
		initData(tagDefinition);
	}
	
	private void initData(TagDefinition tagDefinition) {
		propertyDefDataProvider.getItems().addAll(tagDefinition.getUserDefinedPropertyDefinitions());
		tfName.setValue(tagDefinition.getName());
		colorPicker.setValue(new Color(Integer.valueOf(tagDefinition.getColor())));
	}
	
	@Override
	protected void propertyDefinitionAdded(PropertyDefinition propertyDefinition) {
		super.propertyDefinitionAdded(propertyDefinition);
		tagDefinition.addUserDefinedPropertyDefinition(propertyDefinition);
	}
	
	@Override
	protected void propertyDefinitionRemoved(PropertyDefinition propertyDefinition) {
		super.propertyDefinitionRemoved(propertyDefinition);
		tagDefinition.removeUserDefinedPropertyDefinition(propertyDefinition);
	}
	
	@Override
	protected boolean isWithTagsetSelection() {
		return true;
	}

	@Override
	protected boolean isWithParentSelection() {
		return true;
	}

	@Override
	protected TagDefinition getResult() {
		tagDefinition.setName(tfName.getValue());
		tagDefinition.setColor(String.valueOf(colorPicker.getValue().getRGB()));
		
		for (PropertyDefinition propertyDefinition : propertyDefDataProvider.getItems()) {
			if (!tagDefinition.getUserDefinedPropertyDefinitions().contains(propertyDefinition)) {
				tagDefinition.removeUserDefinedPropertyDefinition(propertyDefinition);
			}
		}
		
		return tagDefinition;
	}

	@Override
	protected String getOkCaption() {
		return "Save";
	}
}
