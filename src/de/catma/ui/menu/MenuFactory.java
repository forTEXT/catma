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
import com.vaadin.ui.Panel;

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
	 * Creates a {@link Menu}, with visibility set to <code>false</code> by default.
	 * @param componentContainer {@link ComponentContainer} The container for the menu.
	 * @param viewContainer {@link Panel} The container for the views.
	 * @param menuEntryDefintions {@link MenuEntryDefinition}
	 * @return {@link Menu}
	 */
	public Menu createMenu(
			final ComponentContainer componentContainer,
			final Panel viewContainer,
			final MenuEntryDefinition... menuEntryDefintions) {
		
		return createMenu(false, componentContainer, viewContainer, menuEntryDefintions);
	}

	/**
	 * Creates a {@link Menu}.
	 * @param isVisible {@link Boolean} Whether or not the menu is visible by default.
	 * @param componentContainer {@link ComponentContainer} The container for the menu.
	 * @param viewContainer {@link Panel} The container for the views.
	 * @param menuEntryDefintions {@link MenuEntryDefinition}
	 * @return {@link Menu}
	 */
	public Menu createMenu(
			final boolean isVisible,
			final ComponentContainer componentContainer,
			final Panel viewContainer,
			final MenuEntryDefinition... menuEntryDefintions) {
		
		
		final MenuBar menuBar = new MenuBar();
		menuBar.setVisible(isVisible);
		
		Menu menu = new Menu(menuBar);
		
		for (final MenuEntryDefinition menuEntryDefinition : menuEntryDefintions) {
			Command command = new Command() {
				
				public void menuSelected(MenuItem selectedItem) {
					viewContainer.setContent(menuEntryDefinition.getView());
				}
			};
			menu.addEntry(menuEntryDefinition.getView(), command);
			menuBar.addItem(menuEntryDefinition.getCaption(), command);
		}
		componentContainer.addComponent(menuBar);
		
		return menu;
	}
	
	
}
