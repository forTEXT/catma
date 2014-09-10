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
package de.catma.ui.dialog;

import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.ui.AbstractField;

import de.catma.ui.client.ui.tagmanager.ColorFieldServerRpc;


public class ColorField extends AbstractField<String> {

	private ColorFieldServerRpc rpc = 
			new ColorFieldServerRpc() {
				@Override
				public void colorChanged(String hexColor) {
					setValue(hexColor, false);
				}
			};
	
	public ColorField(String hexColor) {
		if ((hexColor != null) && (!hexColor.isEmpty())) {
			setValue(hexColor);
		}
	}
	
	public ColorField() {
		registerRpc(rpc);
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}
	
	@Override
	public void setValue(String newValue) throws ReadOnlyException,
			ConversionException {
		super.setValue(newValue, true);
	}
	
	public String getHexColor() {
		return getValue().toString();
	}
	
}
