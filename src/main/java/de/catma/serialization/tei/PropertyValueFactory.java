/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

package de.catma.serialization.tei;

import java.util.List;



/**
 * A factory that sets and gets values for a {@link CProperty}.
 *
 * @author Marco Petris
 *
 */
public interface PropertyValueFactory {
    
	public static final String CATMA_SYSTEM_PROPERTY_PREFIX = "catma_";
    
	/**
	 * Getter.
	 * @param teiElement the element that represents the {@link CProperty}
	 * @return the value of the {@link CProperty} represented by the given element.
	 */
	public String getValue();

	/**
	 * Setter.
	 * @param teiElement the element that represents the {@link CProperty}
	 * @param value the value of the {@link CProperty} represented by the given element
	 */
	public void setValue(Object value );
	
	public List<String> getValueAsList();
	
	public boolean isSingleSelectValue();
	
}
