package de.catma.ui.menu;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Panel;

public class MainMenu extends MenuBar {
	private HashMap<MenuBar.MenuItem, Component> entries;
	
	private Panel viewContainer;
	
	public MainMenu(Panel viewContainer) {
		super();
		
		this.viewContainer = viewContainer;
		
		this.addStyleName("catma-menu");
		
		entries = new HashMap<MenuBar.MenuItem, Component>();
	}
	
	private MenuBar.Command commonCommand = new MenuBar.Command() {
		MenuItem previous = null;
		
		@Override
		public void menuSelected(MenuItem selectedItem) {
			if (previous != null) {
				previous.setStyleName(null);
			}
			
			selectedItem.setStyleName("highlight");
	        previous = selectedItem;
	        
	        Component component = entries.get(selectedItem);
	        
	        viewContainer.setContent(component);
	        
	        fireMenuItemSelectedEvent(component);
		}
	};
	
	public void addEntry(String caption, Component component) {		
		
		MenuBar.MenuItem menuItem = super.addItem(caption, null, commonCommand); //NB: have to call this overload of addItem, otherwise we end up in our overridden, not implemented method below
		
		this.entries.put(menuItem, component);
	}
	
	public void executeEntry(Component component) {
		for (Map.Entry<MenuBar.MenuItem, Component> entry : this.entries.entrySet()) {
			if (entry.getValue() == component) {
				MenuItem menuItem = entry.getKey();
				menuItem.getCommand().menuSelected(menuItem);
				return;
			}
		}
		
		throw new IllegalArgumentException("Component not present in menu");
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
	
	// MenuItemSelectedEvent
	public interface MenuItemSelectedListener {
		public void menuItemSelected(MenuItemSelectedEvent event);
	}
	
	private List<MenuItemSelectedListener> listeners = null;
	
	public class MenuItemSelectedEvent {
		
		final Component component;
		
		public MenuItemSelectedEvent(Component component) {
			this.component = component;
		}
		
		public Component getComponent() {
			return component;
		}
		
	}
	
	private void fireMenuItemSelectedEvent(Component component) {
		if (listeners != null) {
			MenuItemSelectedEvent event = new MenuItemSelectedEvent(component);
			
			for (MenuItemSelectedListener listener : listeners) {
				listener.menuItemSelected(event);
			}
		}
	}
	
	public void addMenuItemSelectedListener(MenuItemSelectedListener listener) {
		if (listeners == null) {
			// need to use a CopyOnWriteArrayList to avoid ConcurrentModificationException
			// this happens because TagResultsDialog adds a listener for MenuItemSelectedEvent
			// and closes its Window when the user navigates, which in turn causes TagResultsDialog
			// to attempt to remove its MenuItemSelectedEvent listener
			listeners = new CopyOnWriteArrayList<MenuItemSelectedListener>();
		}
		listeners.add(listener);
	}
	
	public void removeMenuItemSelectedListener(MenuItemSelectedListener listener) {
		if (listeners == null) {
			return;
		}
		listeners.remove(listener);
	}
	
	
	
	@Override
	public MenuBar.MenuItem addItem(String caption, MenuBar.Command command) {
		throw new UnsupportedOperationException("Use the addEntry method");
	}
	
	@Override
	public MenuBar.MenuItem addItem(String caption, Resource icon,
			MenuBar.Command command) {
		// TODO: create an addEntry overload that accepts an icon
		throw new UnsupportedOperationException("Not implemented");
	}
	
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
