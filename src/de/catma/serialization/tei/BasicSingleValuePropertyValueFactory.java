package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

public abstract class BasicSingleValuePropertyValueFactory implements PropertyValueFactory {

	private TeiElement teiElement;
	
	public BasicSingleValuePropertyValueFactory(TeiElement teiElement) {
		super();
		this.teiElement = teiElement;
	}
	
	protected TeiElement getTeiElement() {
		return teiElement;
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
