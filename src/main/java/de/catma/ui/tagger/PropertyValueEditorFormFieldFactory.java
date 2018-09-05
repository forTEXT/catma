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
package de.catma.ui.tagger;

import java.util.Set;

import com.vaadin.v7.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.FormFieldFactory;
import com.vaadin.v7.ui.ListSelect;

public class PropertyValueEditorFormFieldFactory implements FormFieldFactory {
	
	private ListSelect valueBox;
	
	public PropertyValueEditorFormFieldFactory(
			Set<String> initialValues) {
		valueBox = new ListSelect(
				Messages.getString("PropertyValueEditorFormFieldFactory.Values"),  //$NON-NLS-1$
				initialValues);
		valueBox.setNewItemsAllowed(true);
		valueBox.setMultiSelect(true);
		valueBox.setSizeFull();
	}

	public Field createField(Item item, Object propertyId, Component uiContext) {
		return valueBox;
	}
}
