package de.catma.ui.client.ui.tag.serialization;

import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class JSONUtil {

	public static String getValueFromStringObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			return ((JSONString)jsonValue).stringValue();
		}
		else {
			return null;
		}
	}
}
