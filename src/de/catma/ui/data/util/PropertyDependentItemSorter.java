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
package de.catma.ui.data.util;

import java.util.Comparator;
import java.util.HashMap;

import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.util.DefaultItemSorter;

public class PropertyDependentItemSorter extends DefaultItemSorter {
	
	private HashMap<Object, Comparator<Object>> propertyComparators = 
			new HashMap<Object, Comparator<Object>>();
	
	public PropertyDependentItemSorter() {
	}

	public PropertyDependentItemSorter(
			Object[] propertyIds, Comparator<Object> comparator) {
		for (Object propertyId : propertyIds) {
			setPropertyComparator(propertyId, comparator);
		}
	}

	public PropertyDependentItemSorter(
			Object propertyId, Comparator<Object> comparator) {
		setPropertyComparator(propertyId, comparator);
	}
	
	public PropertyDependentItemSorter(
			Comparator<Object> defaultPropertyValueComparator) {
		super(defaultPropertyValueComparator);
	}

	public void setPropertyComparator(
			Object propertyId, Comparator<Object> comparator) {
		if (comparator == null) {
			propertyComparators.remove(propertyId);
		}
		else {
			propertyComparators.put(propertyId, comparator);
		}
	}
	
    protected int compareProperty(Object propertyId, boolean sortDirection,
            Item item1, Item item2) {
    	if (propertyComparators.containsKey(propertyId)) {
	        // Get the properties to compare
	        final Property<?> property1 = item1.getItemProperty(propertyId);
	        final Property<?> property2 = item2.getItemProperty(propertyId);
	
	        // Get the values to compare
	        final Object value1 = (property1 == null) ? null : property1.getValue();
	        final Object value2 = (property2 == null) ? null : property2.getValue();
	
	        // Result of the comparison
	        int r = 0;
	        Comparator<Object> comparator = propertyComparators.get(propertyId);
	        if (sortDirection) {
	            r = comparator.compare(value1, value2);
	        } else {
	            r = comparator.compare(value2, value1);
	        }
	        return r;
    	}
    	else {
    		return super.compareProperty(
    				propertyId, sortDirection, item1, item2);
    	}
    }
}
