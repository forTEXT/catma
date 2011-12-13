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

import nu.xom.Elements;
import de.catma.core.tag.Property;

/**
 * A factory sets and gets numeric {@link Property} values. TEI: &lt;numeric&gt;
 *
 * @author Marco Petris
 *
 */
public class NumericPropertyValueFactory extends BasicSingleValuePropertyValueFactory {
	
	public NumericPropertyValueFactory(TeiElement propertyElement) {
		super(propertyElement);
	}


	public String getValue() {
		Elements elements = getTeiElement().getChildElements();
		return ((TeiElement)elements.get( 0 )).getAttributeValue( 
					Attribute.numeric_value );
	}
	

	public void setValue(Object value ) {
		Elements elements = getTeiElement().getChildElements();
		elements.get( 0 ).getAttribute( 
			Attribute.numeric_value.getLocalName() ).setValue( value.toString() );
	}
}
