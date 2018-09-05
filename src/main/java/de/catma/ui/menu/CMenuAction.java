package de.catma.ui.menu;

import com.vaadin.event.Action;
import com.vaadin.server.Resource;

public abstract class CMenuAction<T> extends Action {

	public CMenuAction(String caption, Resource icon) {
		super(caption, icon);
	}

	public CMenuAction(String caption) {
		super(caption);
	}
	
	public abstract void handle(T item);

}
