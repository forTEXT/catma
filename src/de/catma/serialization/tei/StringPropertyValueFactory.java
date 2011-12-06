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

/**
 * A factory sets and gets string Property values. TEI: &lt;string&gt;
 *
 * @author Marco Petris
 *
 */
public class StringPropertyValueFactory implements PropertyValueFactory {

	/* (non-Javadoc)
	 * @see org.catma.tag.PropertyValueFactory#getValue(org.catma.tei.TeiElement)
	 */
	public Object getValue( TeiElement teiElement ) {
		Elements elements = teiElement.getChildElements();
		
		return elements.get( 0 ).getValue();
	}
	
	/* (non-Javadoc)
	 * @see org.catma.tag.PropertyValueFactory#setValue(org.catma.tei.TeiElement, java.lang.Object)
	 */
	public void setValue( TeiElement teiElement, Object value ) {
		Elements elements = teiElement.getChildElements();
		TeiElement stringElement = (TeiElement)elements.get( 0 );
		stringElement.removeChildren();
		stringElement.appendChild( value.toString() );
	}

}
