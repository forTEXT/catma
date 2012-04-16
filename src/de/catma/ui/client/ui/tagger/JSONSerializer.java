package de.catma.ui.client.ui.tagger;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class JSONSerializer {

	protected int getIntValueFromStringObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			double result = ((JSONNumber)jsonValue).doubleValue();
			return Double.valueOf(result).intValue();
		}
		else {
			throw new IllegalArgumentException("jsonValue cannot be null");
		}
	}
	
	protected String getStringValueFromStringObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			String result = ((JSONString)jsonValue).stringValue();
			return result;
		}
		else {
			return null;
		}
	}
}
