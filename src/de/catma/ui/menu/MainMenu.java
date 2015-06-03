package de.catma.ui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;

public class MainMenu extends MenuBar {
	private HashMap<MenuBar.MenuItem, Component> entries;
	
	public MainMenu() {
		super();
		
		this.addStyleName("catma-menu");
		
		entries = new HashMap<MenuBar.MenuItem, Component>();
	}
	
	public HashMap<MenuBar.MenuItem, Component> getEntries() {
		return this.entries;
	}
	
	public void addEntry(String caption, MenuBar.Command command, Component component) {
		MenuBar.MenuItem menuItem = super.addItem(caption, command);
		this.entries.put(menuItem, component);
	}
	
	public void executeEntry(Component component) {
		for (Map.Entry<MenuBar.MenuItem, Component> entry : this.entries.entrySet()) {
			if (entry.getValue() == component) {
				MenuItem menuItem = entry.getKey();
				menuItem.getCommand().menuSelected(menuItem);
			}
		}
		
//		throw new IllegalArgumentException("Component not present in menu");
	}
	
	public PropertyChangeListener userChangeListener = 
			new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getNewValue() != null) {
				setVisible(true);
			}
			else {
				setVisible(false);
			}
		}
	};
	
	
	
//	@Override
//	public MenuBar.MenuItem addItem(String caption, MenuBar.Command command) {
//		throw new UnsupportedOperationException("Use the addEntry method");
//	}
	
//	@Override
//	public MenuBar.MenuItem addItem(String caption, Resource icon,
//			MenuBar.Command command) {
//		// TODO: create an addEntry overload that accepts an icon
//		throw new UnsupportedOperationException("Not implemented");
//	}
	
	@Override
	public MenuBar.MenuItem addItemBefore(String caption, Resource icon,
            MenuBar.Command command, MenuBar.MenuItem itemToAddBefore) {
		// TODO: create addEntryBefore
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public void removeItem(MenuBar.MenuItem item) {
		// TODO: create removeEntry
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public void removeItems() {
		// TODO: create removeEntries
		throw new UnsupportedOperationException("Not implemented");
	}
}
