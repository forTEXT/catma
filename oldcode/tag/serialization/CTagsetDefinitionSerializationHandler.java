package de.catma.ui.client.ui.tag.serialization;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

import de.catma.ui.client.ui.tag.CTagsetDefinition;
import de.catma.ui.client.ui.tag.CVersion;
import de.catma.ui.client.ui.tag.serialization.shared.TagsetDefinitionSerializationKey;

public class CTagsetDefinitionSerializationHandler {

	private String tagsetDefinitionJSString;

	public CTagsetDefinitionSerializationHandler(String tagsetDefinionJSString) {
		super();
		this.tagsetDefinitionJSString = tagsetDefinionJSString;
	}
	
	public CTagsetDefinition toCTagsetDefinition() {
		
		JSONObject tagsetDefinitionJS = 
				(JSONObject)JSONParser.parseStrict(tagsetDefinitionJSString);
		
		CTagsetDefinition tagsetDefinition = 
			new CTagsetDefinition(
				((JSONString)tagsetDefinitionJS.get(
						TagsetDefinitionSerializationKey.id.name())).stringValue(),
				((JSONString)tagsetDefinitionJS.get(
						TagsetDefinitionSerializationKey.name.name())).stringValue(),
				new CVersion()); //TODO versioning
		
		JSONArray tagDefinitionsJS = 
				(JSONArray)tagsetDefinitionJS.get(
						TagsetDefinitionSerializationKey.tagDefinitions.name());
		
		for (int i=0; i<tagDefinitionsJS.size(); i++) {
			tagsetDefinition.addTagDefinition(
					new CTagDefinitionSerializationHandler(
							(JSONObject)tagDefinitionsJS.get(i)).toCTagDefinition());
		}
		
		
		return tagsetDefinition;
	}
	
}
