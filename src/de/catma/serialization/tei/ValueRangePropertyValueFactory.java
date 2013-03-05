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
package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Elements;
import de.catma.ExceptionHandler;

public class ValueRangePropertyValueFactory implements PropertyValueFactory {
	
	private boolean singleSelectValue = true;
	private TeiElement teiElement;
	private List<String> value;
	
	public ValueRangePropertyValueFactory(TeiElement teiElement) {
		super();
		this.teiElement = teiElement;
		this.value = new ArrayList<String>();
		extractValue();
	}

	private void extractValue() {
		
		Elements elements = teiElement.getChildElements();
		TeiElement vRange = (TeiElement)elements.get(0);
		
		Elements children = vRange.getChildElements();
		for (int i=0; i<children.size(); i++) {
			try {
				TeiElement curChild = (TeiElement)children.get(i);
				if (curChild.is(TeiElementName.numeric)) {
					value.add(new NumericPropertyValueFactory(vRange).getValue());
				}
				else if (curChild.is(TeiElementName.string)) {
					value.add(new StringPropertyValueFactory(vRange).getValue());
				}
				else {
					throw new UnknownElementException(curChild.getLocalName() + " is not supported!");
				}
			} catch (UnknownElementException e) {
				ExceptionHandler.log(e);
			}
		}
		
		singleSelectValue = !(value.size() > 1);
	}

	public String getValue() {
	
		StringBuilder builder = new StringBuilder();
		
		List<String> list = getValueAsList();
		for (int i=0; i<list.size(); i++) {
			if (i>1) {
				builder.append(",");
			}
			builder.append(list.get(i));
		}
		
		if (list.size() > 0) {
			return builder.toString();
		}
		else {
			return null;
		}
	}

	public void setValue(Object value) {

		// TODO: implement
	}

	public List<String> getValueAsList() {
		return value;
	}

	public boolean isSingleSelectValue() {
		return singleSelectValue;
	}

}
