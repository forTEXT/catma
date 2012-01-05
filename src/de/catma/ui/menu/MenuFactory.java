package de.catma.ui.menu;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Window;

public class MenuFactory {
	
	public MenuFactory() {
	}

	public void createMenu(
			final ComponentContainer componentContainer, 
			final Window repositoryManagerWindow) {
		MenuBar menuBar = new MenuBar();
		menuBar.addItem("Repository Manager", new Command() {
			
			public void menuSelected(MenuItem selectedItem) {
				if (repositoryManagerWindow.getParent() != null) {
					repositoryManagerWindow.bringToFront();
				}
				else {
					componentContainer.getWindow().addWindow(repositoryManagerWindow);
					repositoryManagerWindow.center();
				}
			}
		});
		componentContainer.addComponent(menuBar);
	}
	
	
}
