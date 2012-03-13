package de.catma.ui.dialog;

import com.vaadin.data.util.AbstractProperty;

public class StringProperty extends AbstractProperty {
	
	private String value;
	
	public StringProperty(String value) {
		super();
		this.value = value;
	}
	
	public StringProperty() {
		this("");
	}

	public Object getValue() {
		return value;
	}
	
	public void setValue(Object newValue) throws ReadOnlyException,
			ConversionException {
		value = newValue.toString();
	}

	public Class<?> getType() {
		return String.class;
	}

}
