package de.catma.ui.repository;

import com.vaadin.ui.Window;

import de.catma.ui.CatmaWindow;


public class RepositoryManagerWindow extends CatmaWindow {

	public RepositoryManagerWindow(RepositoryManagerView view) {
		super("Repository Manager");
		this.setContent(view);
		setWidth("75%");
		setHeight("90%");
		setPositionX(50);
		setPositionY(50);
		setEnableScrolling(true);
	}
	
	@Override
	public void setPosition() {
		
		Window mainWindow = getApplication().getMainWindow();
		if ((mainWindow.getWidthUnits() == UNITS_PIXELS) && (mainWindow.getWidth() > 0)) {
			setPositionX(Float.valueOf(Math.min(mainWindow.getWidth(), 50)).intValue());
			
			if ((mainWindow.getHeightUnits() == UNITS_PIXELS) && (mainWindow.getHeight() > 0)) {
				setPositionY(Float.valueOf(Math.min(mainWindow.getHeight(), 50)).intValue());
			}
		}
		else {
			super.setPosition();
		}
	}
}
