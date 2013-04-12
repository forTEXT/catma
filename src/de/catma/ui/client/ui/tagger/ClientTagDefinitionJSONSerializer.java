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
package de.catma.ui.client.ui.tagger;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.util.JSONSerializer;

public class ClientTagDefinitionJSONSerializer extends JSONSerializer {

	public ClientTagDefinition fromJSON(String tagDefJSONString) {
		JSONObject tagDefJSON =
				(JSONObject)JSONParser.parseStrict(tagDefJSONString);
		
		String id = getStringValueFromStringObject(
			tagDefJSON.get(
				ClientTagDefinition.SerializationField.tagDefinitionID.name()));
		String hexColorString = getStringValueFromStringObject(
			tagDefJSON.get(
				ClientTagDefinition.SerializationField.colorHexValue.name()));
		return new ClientTagDefinition(id, hexColorString);
	}
}
