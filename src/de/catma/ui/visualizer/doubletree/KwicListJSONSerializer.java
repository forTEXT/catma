package de.catma.ui.visualizer.doubletree;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.catma.document.source.KeywordInContext;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.TermInfo;
import de.catma.ui.client.ui.visualizer.shared.KwicSerializationField;
import de.catma.ui.data.util.JSONSerializationException;

public class KwicListJSONSerializer {
	
	public String toJSON(List<KeywordInContext> kwicList, boolean caseSensitive) 
				throws JSONSerializationException {
		
		JSONObject kwicListJson = new JSONObject();
		JSONArray prefixArraysJson = new JSONArray();
		JSONArray tokenArray = new JSONArray();
		JSONArray postfixArraysJson = new JSONArray();
		try {
			kwicListJson.put(KwicSerializationField.prefixArrays.name(), prefixArraysJson);
			kwicListJson.put(KwicSerializationField.tokenArray.name(), tokenArray);
			kwicListJson.put(KwicSerializationField.postfixArrays.name(), postfixArraysJson);
			kwicListJson.put(
					KwicSerializationField.caseSensitive.name(), 
					Boolean.toString(caseSensitive));
			for (KeywordInContext kwic : kwicList) {
				if (kwic instanceof KeywordInSpanContext) {
					KeywordInSpanContext spanKwic = (KeywordInSpanContext)kwic;
					
					JSONArray prefixArrayJson = new JSONArray();
					prefixArraysJson.put(prefixArrayJson);
					
					for (TermInfo ti : spanKwic.getSpanContext().getBackwardTokens()) {
						prefixArrayJson.put(ti.getTerm());
					}
					
					tokenArray.put(spanKwic.getKeyword());
					
					JSONArray postfixArrayJson = new JSONArray();
					postfixArraysJson.put(postfixArrayJson);
					
					for (TermInfo ti : spanKwic.getSpanContext().getForwardTokens()) {
						postfixArrayJson.put(ti.getTerm());
					}
				}
			}
			
			
			return kwicListJson.toString();
		}
		catch (JSONException je) {
			throw new JSONSerializationException(je);
		}
		
		
	}
}
