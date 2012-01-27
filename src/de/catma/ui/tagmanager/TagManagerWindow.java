package de.catma.ui.tagmanager;

import com.vaadin.ui.Window;

import de.catma.ui.CatmaWindow;

public class TagManagerWindow extends CatmaWindow {

	public TagManagerWindow(TagManagerView tagManagerView) {
		super("Tag Manager");
		this.setContent(tagManagerView);
		setWidth("35%");
		setHeight("90%");
	}
	
	@Override
	public void setPosition() {
		Window mainWindow = getApplication().getMainWindow();
		if ((mainWindow.getWidthUnits() == UNITS_PIXELS) && (mainWindow.getWidth() > 0)) {
			
			int posX = Float.valueOf(mainWindow.getWidth()-(mainWindow.getWidth()*38/100)).intValue();
			if (posX > 0) {
				setPositionX(posX);
				if ((mainWindow.getHeightUnits() == UNITS_PIXELS) && (mainWindow.getHeight() > 0)) {
					setPositionY(Float.valueOf(Math.min(mainWindow.getHeight(), 20)).intValue());
				}
			}
			else {
				super.setPosition();
			}
		}
		else {
			super.setPosition();
		}
	}
}
