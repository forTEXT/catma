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

import java.util.HashMap;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Panel;
import com.vaadin.ui.MenuBar.MenuItem;

import de.catma.ui.tabbedview.TabbedView;

public class MenuFactory {
	
	public static class MenuEntryDefinition {
		private String caption;
		private TabbedView view;
		
		public MenuEntryDefinition(String caption, TabbedView view) {
			super();
			this.caption = caption;
			this.view = view;
		}
		
		public String getCaption() {
			return caption;
		}
		
		public TabbedView getView() {
			return view;
		}
	}
	
	public MenuFactory() {
	}
	
	/**
	 * Creates a {@link MainMenu}, with visibility set to <code>false</code> by default.
	 * @param componentContainer {@link ComponentContainer} The container for the menu.
	 * @param viewContainer {@link Panel} The container for the views.
	 * @param menuEntryDefinitions {@link MenuEntryDefinition}
	 * @return {@link MainMenu}
	 */
	public MainMenu createMenu(
			final ComponentContainer componentContainer,
			final Panel viewContainer,
			final MenuEntryDefinition... menuEntryDefinitions) {
		
		return createMenu(false, componentContainer, viewContainer, menuEntryDefinitions);
	}

	/**
	 * Creates a {@link MainMenu}.
	 * @param isVisible {@link Boolean} Whether or not the menu is visible by default.
	 * @param componentContainer {@link ComponentContainer} The container for the menu.
	 * @param viewContainer {@link Panel} The container for the views.
	 * @param menuEntryDefinitions {@link MenuEntryDefinition}
	 * @return {@link MainMenu}
	 */
	public MainMenu createMenu(
			final boolean isVisible,
			final ComponentContainer componentContainer,
			final Panel viewContainer,
			final MenuEntryDefinition... menuEntryDefinitions) {
		
		
		final MainMenu mainMenu = new MainMenu();
		mainMenu.setVisible(isVisible);
		
		MenuBar.Command command = new MenuBar.Command() {
			MenuItem previous = null;
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				if (previous != null) {
					previous.setStyleName(null);
				}
				
				selectedItem.setStyleName("highlight");
		        previous = selectedItem;
		        
		        HashMap<MenuBar.MenuItem, Component> entries = mainMenu.getEntries();
		        Component component = entries.get(selectedItem);
		        
		        viewContainer.setContent(component);
			}
		};
		
		for (final MenuEntryDefinition menuEntryDefinition : menuEntryDefinitions) {
			mainMenu.addEntry(menuEntryDefinition.getCaption(), command, menuEntryDefinition.getView());
		}
		
		componentContainer.addComponent(mainMenu);
		
		return mainMenu;
	}
	
	
}
