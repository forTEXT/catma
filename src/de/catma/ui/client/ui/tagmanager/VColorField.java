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
package de.catma.ui.client.ui.tagmanager;

import net.auroris.ColorPicker.client.ColorPicker;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.ui.Field;

import de.catma.ui.client.ui.tagmanager.shared.ColorFieldMessageAttribute;

public class VColorField extends Composite implements Paintable, Field {
	
	private ColorPicker colorPicker;
	private ApplicationConnection serverConnection;
	private String clientID;
	
	public VColorField() {
		colorPicker = new ColorPicker() {
			@Override
			public void onChange(Widget sender) {
				super.onChange(sender);
				serverConnection.updateVariable(
						clientID, 
						ColorFieldMessageAttribute.COLOR_SET.name(),
						getHexColor(),
						true);
			}
		};
		int [] randomColor = getRandomColor();
		try {
			colorPicker.setRGB( randomColor[0], randomColor[1], randomColor[2]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		initWidget(colorPicker);
	}
	

	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        if (client.updateComponent(this, uidl, true)) {
            return;
        }

        this.serverConnection = client;
        this.clientID = uidl.getId();

        if (uidl.hasAttribute(ColorFieldMessageAttribute.COLOR_SET.name())) {
        	try {
        		String hexColor = uidl.getStringAttribute(
						ColorFieldMessageAttribute.COLOR_SET.name());
				colorPicker.setHex(hexColor);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}
	
	private int[] getRandomColor() {
		int i = Random.nextInt(3);
		switch(i) {
			case 0 : {
				return new int[] { 255, 0, 0};
			}
			case 1 : {
				return new int[] { 0, 255, 0};
			}
			case 2 : {
				return new int[] { 0, 0, 255};
			}
			default : {
				return new int[] { 0, 0, 255};
			}
		}
	}
}
