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

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.Button;
import com.vaadin.ui.ClientWidget;
import com.vaadin.ui.ClientWidget.LoadStyle;

import de.catma.ui.client.ui.tagmanager.VColorButton;


@ClientWidget(value = VColorButton.class, loadStyle = LoadStyle.EAGER)
public class ColorButton extends Button {

	private String color;
	
	public ColorButton(String color, ClickListener listener) {
		super("", listener);
		this.color = color;
	}

	public void setColor(String color) {
		this.color = color;
		requestRepaint();
	}
	
	@Override
	public synchronized void paintContent(PaintTarget target)
			throws PaintException {
		super.paintContent(target);
		
		target.addAttribute(VColorButton.COLOR_ATTRIBUTE, color);
	}

	
}
