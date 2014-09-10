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
package de.catma.ui.tagmanager;

import com.vaadin.ui.Window;

import de.catma.ui.CatmaWindow;

public class TagManagerWindow extends CatmaWindow {

	public TagManagerWindow(TagManagerView tagManagerView) {
		super("Tag Manager");
		this.setContent(tagManagerView);
		setWidth("35%");
		setHeight("85%");
	}
	//TODO: vaadin7
//	
//	@Override
//	public void setPosition() {
//		Window mainWindow = getApplication().getMainWindow();
//		if ((mainWindow.getWidthUnits() == UNITS_PIXELS) && (mainWindow.getWidth() > 0)) {
//			
//			int posX = Float.valueOf(mainWindow.getWidth()-(mainWindow.getWidth()*38/100)).intValue();
//			if (posX > 0) {
//				setPositionX(posX);
//				if ((mainWindow.getHeightUnits() == UNITS_PIXELS) && (mainWindow.getHeight() > 0)) {
//					setPositionY(Float.valueOf(Math.min(mainWindow.getHeight(), 20)).intValue());
//				}
//			}
//			else {
//				super.setPosition();
//			}
//		}
//		else {
//			super.setPosition();
//		}
//	}
}
