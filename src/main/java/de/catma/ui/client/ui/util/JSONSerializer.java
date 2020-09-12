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
package de.catma.ui.client.ui.util;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class JSONSerializer {

	public int getIntValueFromNumberObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			double result = ((JSONNumber)jsonValue).doubleValue();
			return Double.valueOf(result).intValue();
		}
		else {
			throw new IllegalArgumentException("jsonValue cannot be null");
		}
	}
	
	public String getStringValueFromStringObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			String result = ((JSONString)jsonValue).stringValue();
			return result;
		}
		else {
			return null;
		}
	}
	
	public boolean getBooleanValueFromBooleanObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			boolean result = ((JSONBoolean)jsonValue).booleanValue();
			return result;
		}
		else {
			return false;
		}
	}
}
