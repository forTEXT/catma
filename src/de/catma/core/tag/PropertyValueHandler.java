package de.catma.core.tag;

import java.util.List;

public interface PropertyValueHandler<T> {

	public T getValue();
	public void setValue(T value);
	public String getValueAsString();
	public List<T> getValueAsList();
	
}
