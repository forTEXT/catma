package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

public abstract class BasicSingleValuePropertyValueFactory implements PropertyValueFactory {

	public List<String> getValueAsList(TeiElement teiElement) {
		String value = getValue(teiElement);
		ArrayList<String> list = new ArrayList<String>();
		list.add(value);
		return list;
	}
	
	public boolean isSingleSelectValue() {
		return true;
	}
	
}
