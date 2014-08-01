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
package de.catma.ui;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Window;

public class CatmaWindow extends Window {
	
	private boolean enableScrolling = false;
	
	public CatmaWindow() {
		super();
	}

	public CatmaWindow(String caption, ComponentContainer content) {
		super(caption, content);
	}

	public CatmaWindow(String caption) {
		super(caption);
	}

	public void setPosition() {
		center();
	}
	
	public void setEnableScrolling(boolean enableScrolling) {
		this.enableScrolling = enableScrolling;
//		attributes.put(
//				VCatmaWindow.EventAttribute.enableScrolling.name(), 
//				String.valueOf(enableScrolling));
	}
	
	public boolean isEnableScrolling() {
		return enableScrolling;
	}
}
