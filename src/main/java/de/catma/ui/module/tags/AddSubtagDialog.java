package de.catma.ui.module.tags;

import java.util.Collection;
import java.util.Collections;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.dialog.SaveCancelListener;

public class AddSubtagDialog extends AbstractAddEditTagDialog<TagDefinition> {

	/* So I don't have to change all the call to this function while testing, I'm doing two functions w/ different signatures */
	public AddSubtagDialog(
			SaveCancelListener<TagDefinition> saveCancelListener) {
		super("Add Subtag", saveCancelListener);
		initComponents(false);
		initActions();
	}

	public AddSubtagDialog(
			Collection<TagsetDefinition> availableTags,
			Collection<TagDefinition> preSelectedTags,
			SaveCancelListener<TagDefinition> saveCancelListener) {
		super("Add Subtag", saveCancelListener);
		initComponents(availableTags, preSelectedTags, false);
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
