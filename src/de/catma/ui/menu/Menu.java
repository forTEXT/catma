package de.catma.ui.menu;

import java.util.HashMap;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.MenuBar;

public class Menu {

	private MenuBar menuBar;
	
	private HashMap<ComponentContainer, MenuBar.Command> entries;
	
	public Menu(MenuBar menuBar) {
		this.menuBar = menuBar;
		this.entries = new HashMap<ComponentContainer, MenuBar.Command>();
	}
	
	public void addEntry(ComponentContainer compContainer, MenuBar.Command command) {
		this.entries.put(compContainer, command);
	}
	
	public void executeEntry(ComponentContainer compContainer) {
		this.entries.get(compContainer).menuSelected(null);
	}
	
	public MenuBar getMenuBar() {
		return menuBar;
	}
	
}
