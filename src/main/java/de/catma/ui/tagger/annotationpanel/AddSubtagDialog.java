package de.catma.ui.tagger.annotationpanel;

import java.util.Collections;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.Version;
import de.catma.ui.dialog.SaveCancelListener;

public class AddSubtagDialog extends AbstractAddEditTagDialog<TagDefinition> {
	
	public AddSubtagDialog(	SaveCancelListener<TagDefinition> saveCancelListener) {
		super("Add Subtag", saveCancelListener);
		initComponents();
		initActions();
	}

	@Override
	protected boolean isWithTagsetSelection() {
		return false;
	}

	@Override
	protected TagDefinition getResult() {
		
		TagDefinition tag = 
			new TagDefinition(
				null, 
				idGenerator.generate(), 
				tfName.getValue(), new Version(),
				null, 
				null, 
				isWithTagsetSelection()?cbTagsets.getValue().getUuid():null);
		
		tag.addSystemPropertyDefinition(
			new PropertyDefinition(
				idGenerator.generate(), 
				PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
				Collections.singletonList(String.valueOf(colorPicker.getValue().getRGB()))));

		for (PropertyDefinition propertyDefinition : propertyDefDataProvider.getItems()) {
			tag.addUserDefinedPropertyDefinition(propertyDefinition);
		}		
		
		
		return tag;
	}

}
