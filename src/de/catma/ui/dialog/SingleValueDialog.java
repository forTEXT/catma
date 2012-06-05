package de.catma.ui.dialog;

import com.vaadin.data.util.PropertysetItem;
import com.vaadin.ui.Window;

public class SingleValueDialog {
	
	public void getSingleValue(
			Window parent,
			String dialogCaption,
			String requiredErrorMessage,
			SaveCancelListener<PropertysetItem> listener, 
			String valueProperty) {
		
		PropertyCollection propertyCollection = 
				new PropertyCollection(valueProperty);

		FormDialog formDialog =
			new FormDialog(
				dialogCaption,
				propertyCollection,
				listener);
	
		formDialog.getField(
				valueProperty).setRequired(true);
		formDialog.getField(
				valueProperty).setRequiredError(requiredErrorMessage);
		formDialog.show(parent);
	}
}
