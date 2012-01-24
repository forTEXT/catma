package de.catma.ui.menu;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Window;

public class MenuFactory {
	
	public static class MenuEntryDefinition {
		private String caption;
		private Window window;
		
		public MenuEntryDefinition(String caption, Window window) {
			super();
			this.caption = caption;
			this.window = window;
		}
		
		public String getCaption() {
			return caption;
		}
		
		public Window getWindow() {
			return window;
		}
	}
	
	public MenuFactory() {
	}

	public Menu createMenu(
			final ComponentContainer componentContainer, 
			final MenuEntryDefinition... menuEntryDefintions) {
		
		Menu menu = new Menu();
		
		MenuBar menuBar = new MenuBar();
		
		for (final MenuEntryDefinition menuEntryDefinition : menuEntryDefintions) {
			Command command = new Command() {
				
				public void menuSelected(MenuItem selectedItem) {
					if (menuEntryDefinition.getWindow().getParent() != null) {
						menuEntryDefinition.getWindow().bringToFront();
					}
					else {
						componentContainer.getWindow().addWindow(menuEntryDefinition.getWindow());
						menuEntryDefinition.getWindow().center();
					}
				}
			};
			menu.addEntry(menuEntryDefinition.getWindow().getContent(), command);
			menuBar.addItem(menuEntryDefinition.getCaption(), command);
		}
		componentContainer.addComponent(menuBar);
		
		return menu;
	}
	
	
}
