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

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormFieldFactory;

public class TagDefinitionFieldFactory implements FormFieldFactory {
	
	private String colorPropertyId;
	
	public TagDefinitionFieldFactory(String colorPropertyId) {
		this.colorPropertyId = colorPropertyId;
	}

	public Field createField(Item item, Object propertyId, Component uiContext) {
		
		if (propertyId.equals(colorPropertyId)) {
			String colorValue = 
					(String)item.getItemProperty(propertyId).getValue();

			ColorField colorField = new ColorField(colorValue);
			colorField.setCaption(
				DefaultFieldFactory.createCaptionByPropertyId(colorPropertyId));
			return colorField;
		}
		
		
		return DefaultFieldFactory.get().createField(item, propertyId, uiContext);
	}
}
