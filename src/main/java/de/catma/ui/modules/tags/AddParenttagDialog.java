package de.catma.ui.modules.tags;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.Pair;

public class AddParenttagDialog extends AbstractAddEditTagDialog<Pair<TagsetDefinition, TagDefinition>> {

	public AddParenttagDialog(
			Collection<TagsetDefinition> availableTagsets, 
			Optional<TagsetDefinition> preSelectedTagset, 
			SaveCancelListener<Pair<TagsetDefinition, TagDefinition>> saveCancelListener) {
		super("Add Tag", saveCancelListener);
		initComponents(availableTagsets, preSelectedTagset, false);
		initActions();
	}

	@Override
	protected boolean isWithTagsetSelection() {
		return true;
	}
	
	@Override
	protected String getOkCaption() {
		return "Add Tag";
	}
	
	@Override
	protected Pair<TagsetDefinition, TagDefinition> getResult() {
		TagDefinition tag = 
			new TagDefinition(
				null, 
				idGenerator.generate(), 
				tfName.getValue(), new Version(),
				null, 
				null, 
				cbTagsets.getValue().getUuid());
		
		tag.addSystemPropertyDefinition(
			new PropertyDefinition(
				idGenerator.generate(), 
				PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
				Collections.singletonList(String.valueOf(colorPicker.getValue().getRGB()))));

		for (PropertyDefinition propertyDefinition : propertyDefDataProvider.getItems()) {
			tag.addUserDefinedPropertyDefinition(propertyDefinition);
		}
		
		return new Pair<>(cbTagsets.getValue(), tag);
	}

}
