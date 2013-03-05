/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
