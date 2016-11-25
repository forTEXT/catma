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

import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.client.ui.util.JSONSerializer;

public class TextRangeJSONSerializer extends JSONSerializer {

	public TextRange fromJSON(String jsonString) {
		JSONObject trJSON = (JSONObject)JSONParser.parseStrict(jsonString);
		TextRange tr = 
				new TextRange(
					getIntValueFromStringObject(
						trJSON.get(SerializationField.startPos.name())),
					getIntValueFromStringObject(
						trJSON.get(SerializationField.endPos.name())));
		return tr;
	}
	
	public String toJSONArrayString(List<TextRange> textRanges) {
		return toJSONArray(textRanges).toString();
	}
	
	public JSONArray toJSONArray(List<TextRange> textRanges) {
		JSONArray rangesJSON = new JSONArray();
		int i=0;
		for (TextRange tr : textRanges) {
			JSONObject trJSON = toJSON(tr);
			rangesJSON.set(i, trJSON);
			i++;
		}
		
		return rangesJSON;
	}
	
	private JSONObject toJSON(TextRange tr) {
		JSONObject trJSON = new JSONObject();
		trJSON.put(
			SerializationField.startPos.name(), 
			new JSONNumber(tr.getStartPos()));
		trJSON.put(
			SerializationField.endPos.name(), 
			new JSONNumber(tr.getEndPos()));
		return trJSON;
	}
}
