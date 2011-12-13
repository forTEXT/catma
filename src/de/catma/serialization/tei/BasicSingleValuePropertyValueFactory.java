package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

public abstract class BasicSingleValuePropertyValueFactory implements PropertyValueFactory {

	private TeiElement propertyElement;
	
	public BasicSingleValuePropertyValueFactory(TeiElement propertyElement) {
		super();
		this.propertyElement = propertyElement;
	}
	
	protected TeiElement getTeiElement() {
		return propertyElement;
	}

	public List<String> getValueAsList() {
		String value = getValue();
		ArrayList<String> list = new ArrayList<String>();
		list.add(value);
		return list;
	}
	
	public boolean isSingleSelectValue() {
		return true;
	}
	
}
