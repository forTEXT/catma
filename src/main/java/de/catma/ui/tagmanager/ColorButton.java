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

import com.vaadin.ui.Button;

import de.catma.ui.client.ui.tagmanager.ColorButtonState;


public class ColorButton extends Button {

	public ColorButton(String color, ClickListener listener) {
		super("", listener); //$NON-NLS-1$
		setColor(color);
	}

	@Override
	public ColorButtonState getState() {
		return (ColorButtonState)super.getState();
	}
	
	public void setColor(String color) {
		getState().color = color;
	}
	
	public String getColor() {
		return getState().color;
	}
	

	
}