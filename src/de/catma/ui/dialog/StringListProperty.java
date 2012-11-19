package de.catma.ui.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.data.util.AbstractProperty;

public class StringListProperty extends AbstractProperty {

	public List<String> list;
	
	public Class<?> getType() {
		return List.class;
	}
	public Object getValue() {
		return list;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setValue(Object newValue) throws ReadOnlyException,
			ConversionException {
		list = new ArrayList<String>();
		list.addAll((Collection)newValue);
	}
	
	public List<String> getList() {
		return list;
	}
}
