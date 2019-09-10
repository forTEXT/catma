package de.catma.ui.client.ui.visualization.doubletree;

import java.util.ArrayList;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import de.catma.ui.client.ui.util.JSONSerializer;
import de.catma.ui.client.ui.visualization.doubletree.shared.KwicSerializationField;

public class KwicList {
	
	private boolean caseSensitive;
	private boolean rightToLeftLanguage;
	private ArrayList<ArrayList<String>> prefixArrays;
	private ArrayList<ArrayList<String>> postfixArrays;
	private ArrayList<String> tokenArray;

	public KwicList() {
		this.prefixArrays = new ArrayList<ArrayList<String>>();
		this.postfixArrays = new ArrayList<ArrayList<String>>();
		this.tokenArray = new ArrayList<String>();
	}
	
	public String[] getTokens() {
		return tokenArray.toArray(new String[]{});
	}
	
	public String[][] getPrefixes() {
		String[][] prefixes = new String[prefixArrays.size()][];
		
		for (int i = 0; i<prefixArrays.size(); i++) {
			prefixes[i] = prefixArrays.get(i).toArray(new String[]{});
		}
		
		return prefixes;
		
	}
	
	public String[][] getPostfixes() {
		String[][] postfixes = new String[postfixArrays.size()][];
		
		for (int i = 0; i<postfixArrays.size(); i++) {
			postfixes[i] = postfixArrays.get(i).toArray(new String[]{});
		}
		
		return postfixes;
		
	}

	public void addToken(String token) {
		tokenArray.add(token);
	}
	
	public void addPrefix(ArrayList<String> prefix) {
		prefixArrays.add(prefix);
	}
	
	public void addPostfix(ArrayList<String> postfix) {
		postfixArrays.add(postfix);
	}
	
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setRightToLeftLanguage(boolean rightToLeftLanguage) {
		this.rightToLeftLanguage = rightToLeftLanguage;
	}

	public boolean isRightToLeftLanguage() {
		return rightToLeftLanguage;
	}
	
	public static KwicList fromJSON(String jsonString){
		KwicList kwicList = new KwicList();
		JSONSerializer serializer = new JSONSerializer();
		
		JSONObject kwicListJson = (JSONObject)JSONParser.parseStrict(jsonString);
		
		JSONArray tokens = 
			(JSONArray)kwicListJson.get(KwicSerializationField.tokenArray.name());
		
		JSONArray prefixes = 
			(JSONArray)kwicListJson.get(KwicSerializationField.prefixArrays.name());	
		
		JSONArray postfixes = 
			(JSONArray)kwicListJson.get(KwicSerializationField.postfixArrays.name());	
		
		kwicList.setCaseSensitive(
			Boolean.parseBoolean(
				serializer.getStringValueFromStringObject(
					kwicListJson.get(KwicSerializationField.caseSensitive.name()))));
		kwicList.setRightToLeftLanguage(
			Boolean.parseBoolean(
				serializer.getStringValueFromStringObject(
					kwicListJson.get(KwicSerializationField.rightToLeftLanguage.name()))));
		
		for (int i=0; i<tokens.size(); i++) {
			
			kwicList.addToken(
				serializer.getStringValueFromStringObject(tokens.get(i)));

			ArrayList<String> prefix = new ArrayList<String>();
			kwicList.addPrefix(prefix);
			
			JSONArray prefixJson = (JSONArray)prefixes.get(i);
			
			for (int pI=0; pI<prefixJson.size(); pI++) {
				prefix.add(serializer.getStringValueFromStringObject(prefixJson.get(pI)));
			}
			
			ArrayList<String> postfix = new ArrayList<String>();
			kwicList.addPostfix(postfix);
			
			JSONArray postfixJson = (JSONArray)postfixes.get(i);
			
			for (int pI=0; pI<postfixJson.size(); pI++) {
				postfix.add(serializer.getStringValueFromStringObject(postfixJson.get(pI)));
			}
		}
		
		
		return kwicList;
	}

}
