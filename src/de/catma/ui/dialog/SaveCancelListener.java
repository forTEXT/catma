package de.catma.ui.dialog;

import com.vaadin.data.util.PropertysetItem;

public interface SaveCancelListener {
	public void savePressed(PropertysetItem propertysetItem);
	public void cancelPressed();
}