package de.catma.ui.repository.sharing;

import com.vaadin.ui.Component;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.ui.DefaultFieldFactory;
import com.vaadin.v7.ui.Field;

import de.catma.ui.data.util.NonEmptySequenceValidator;
import de.catma.ui.field.FormFieldGenerator;

public class UserIdentFieldGenerator implements FormFieldGenerator {

	public Field createField(Item item, Component uiContext) {
		Field field = DefaultFieldFactory.get().createField(item, "userIdentification", uiContext); //$NON-NLS-1$
		field.setCaption(Messages.getString("UserIdentFieldGenerator.shareWith")); //$NON-NLS-1$
		field.setRequired(true);
		field.setInvalidAllowed(false);
		field.addValidator(new NonEmptySequenceValidator(Messages.getString("UserIdentFieldGenerator.enterEmailAddress"))); //$NON-NLS-1$
		return field;
	}

}
