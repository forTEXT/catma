package de.catma.ui.dialog;

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormFieldFactory;

public class TagDefinitionFieldFactory implements FormFieldFactory {
	
	private String colorPropertyId;
	
	public TagDefinitionFieldFactory(String colorPropertyId) {
		this.colorPropertyId = colorPropertyId;
	}

	public Field createField(Item item, Object propertyId, Component uiContext) {
		
		if (propertyId.equals(colorPropertyId)) {
			String colorValue = 
					(String)item.getItemProperty(propertyId).getValue();

			ColorField colorField = new ColorField(colorValue);
			colorField.setCaption(
				DefaultFieldFactory.createCaptionByPropertyId(colorPropertyId));
			return colorField;
		}
		
		
		return DefaultFieldFactory.get().createField(item, propertyId, uiContext);
	}
}
