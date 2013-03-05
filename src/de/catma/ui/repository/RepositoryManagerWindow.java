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
