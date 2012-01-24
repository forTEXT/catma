package de.catma.ui.tagmanager;

import com.vaadin.terminal.ClassResource;

public class TagTreeEntry<T> {
	
	private T value;
	private ClassResource icon;
	
	public TagTreeEntry(T value, ClassResource icon) {
		super();
		this.value = value;
		this.icon = icon;
	}
	
	
	public ClassResource getIcon() {
		return icon;
	}
	
	public T getValue() {
		return value;
	}
	

	public String getCaption() {
		return getValue().toString();
	}
}
