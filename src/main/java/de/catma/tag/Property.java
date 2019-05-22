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
package de.catma.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A property of a {@link TagInstance}. Each property has a {@link PropertyDefinition definition}
 * and a {@link PropertyValueList list of values} containing one or more
 * values.
 * 
 * @author marco.petris@web.de
 *
 */
public class Property {

	private String propertyDefinitionId;
	private List<String> propertyValueList;
	
	public Property(String propertyDefinitionId,
			Collection<String> propertyValueList) {
		this.propertyDefinitionId = propertyDefinitionId;
		this.propertyValueList = new ArrayList<>(propertyValueList);
	}

	public List<String> getPropertyValueList() {
		return Collections.unmodifiableList(propertyValueList);
	}
	
	public String getPropertyDefinitionId() {
		return propertyDefinitionId;
	}

	public void setPropertyValueList(Collection<String> propertyValueList) {
		this.propertyValueList = new ArrayList<String>(propertyValueList);
	}

	/**
	 * The internal list of values is replaced with the possible values
	 * proposed by the definition.
	 * @see PropertyDefinition#getPossibleValueList()
	 */
	@Deprecated
	public void synchronize() {
//		setPropertyValueList(propertyDefinition.getPossibleValueList());
	}

	public String getFirstValue() {
		return propertyValueList.isEmpty()?null:propertyValueList.get(0);
	}
}
