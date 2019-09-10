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
package de.catma.ui.module.project.document;

import java.util.Collection;

import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Table;

public class ComboBoxColumnGenerator implements Table.ColumnGenerator {
	
	private Collection<?> options;	
	private ValueChangeListenerGenerator valueChangeListenerGenerator;
	
	public ComboBoxColumnGenerator(Collection<?> options, ValueChangeListenerGenerator valueChangeListenerGenerator) {
		this.options = options;
		this.valueChangeListenerGenerator = valueChangeListenerGenerator;
	}

	public Component generateCell(Table source, Object itemId, Object columnId) {
		// Get the object stored in the cell as a property
        Property prop = source.getItem(itemId).getItemProperty(columnId);
        
    	ComboBox comboBox = new ComboBox(null, options);
    	comboBox.setNullSelectionAllowed(false);
    	comboBox.setPropertyDataSource(prop);
    	comboBox.setImmediate(true);
    	
    	if(valueChangeListenerGenerator != null){
    		comboBox.addValueChangeListener(valueChangeListenerGenerator.generateValueChangeListener(source, itemId, columnId));
    	}    	

        return comboBox;
	}
}
