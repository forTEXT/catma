package de.catma.ui.repository.sharing;

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;

import de.catma.ui.data.util.NonEmptySequenceValidator;
import de.catma.ui.field.FormFieldGenerator;

public class UserIdentFieldGenerator implements FormFieldGenerator {

	public Field createField(Item item, Component uiContext) {
		Field field = DefaultFieldFactory.get().createField(item, "userIdentification", uiContext);
		field.setCaption("Share with (email)");
		field.setRequired(true);
		field.setInvalidAllowed(false);
		field.addValidator(new NonEmptySequenceValidator("You have to enter an email address!"));
		return field;
	}

}
