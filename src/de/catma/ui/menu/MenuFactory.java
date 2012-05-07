package de.catma.ui.menu;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.ui.CatmaWindow;

public class MenuFactory {
	
	public static class MenuEntryDefinition {
		private String caption;
		private CatmaWindow window;
		
		public MenuEntryDefinition(String caption, CatmaWindow window) {
			super();
			this.caption = caption;
			this.window = window;
		}
		
		public String getCaption() {
			return caption;
		}
		
		public CatmaWindow getWindow() {
			return window;
		}
	}
	
	public MenuFactory() {
	}

	public Menu createMenu(
			final ComponentContainer componentContainer, 
			final MenuEntryDefinition... menuEntryDefintions) {
		
		
		final MenuBar menuBar = new MenuBar();
		Menu menu = new Menu(menuBar);
		
		for (final MenuEntryDefinition menuEntryDefinition : menuEntryDefintions) {
			Command command = new Command() {
				
				public void menuSelected(MenuItem selectedItem) {
					if (menuEntryDefinition.getWindow().getParent() != null) {
						menuEntryDefinition.getWindow().bringToFront();
					}
					else {
						componentContainer.getWindow().addWindow(menuEntryDefinition.getWindow());
						menuEntryDefinition.getWindow().setPosition();
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
