package de.catma.ui.module.tags;

import java.util.Collections;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.Version;
import de.catma.ui.dialog.SaveCancelListener;

public class AddSubtagDialog extends AbstractAddEditTagDialog<TagDefinition> {
	
	public AddSubtagDialog(SaveCancelListener<TagDefinition> saveCancelListener) {
		super("Add Subtag", saveCancelListener);
		initComponents(false);
		initActions();
		initData();
	}

	private void initData() {
		//tfParent.setValue(isWithTagsetSelection()?cbTagsets.getValue().getUuid().toString():"");
	}
	@Override
	protected boolean isWithTagsetSelection() {
		return false;
	}

	@Override
	protected boolean isWithParentSelection() {
		return true;
	}
	@Override
	protected TagDefinition getResult() {
		
		TagDefinition tag = 
			new TagDefinition(
				idGenerator.generate(), 
				tfName.getValue(),
				null, 
				isWithTagsetSelection()?cbTagsets.getValue().getUuid():null);
		
		tag.addSystemPropertyDefinition(
			new PropertyDefinition(
				idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()), 
				PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
				Collections.singletonList(String.valueOf(colorPicker.getValue().getRGB()))));

		for (PropertyDefinition propertyDefinition : propertyDefDataProvider.getItems()) {
			tag.addUserDefinedPropertyDefinition(propertyDefinition);
		}		
		
		
		return tag;
	}

}
