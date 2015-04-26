package de.catma.ui.visualizer.doubletree;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.document.source.KeywordInContext;
import de.catma.indexer.KeywordInSpanContext;
import de.catma.indexer.TermInfo;
import de.catma.ui.client.ui.visualizer.doubletree.shared.KwicSerializationField;

public class KwicListJSONSerializer {
	
	public String toJSON(List<KeywordInContext> kwicList, boolean caseSensitive) {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		
		ObjectNode kwicListJson = factory.objectNode();
		ArrayNode prefixArraysJson = factory.arrayNode();
		ArrayNode tokenArray = factory.arrayNode();
		ArrayNode postfixArraysJson = factory.arrayNode();
		
		kwicListJson.set(KwicSerializationField.prefixArrays.name(), prefixArraysJson);
		kwicListJson.set(KwicSerializationField.tokenArray.name(), tokenArray);
		kwicListJson.set(KwicSerializationField.postfixArrays.name(), postfixArraysJson);
		kwicListJson.put(
				KwicSerializationField.caseSensitive.name(), 
				Boolean.toString(caseSensitive));
		for (KeywordInContext kwic : kwicList) {
			if (kwic instanceof KeywordInSpanContext) {
				KeywordInSpanContext spanKwic = (KeywordInSpanContext)kwic;
				
				ArrayNode prefixArrayJson = factory.arrayNode();
				prefixArraysJson.add(prefixArrayJson);
				
				for (TermInfo ti : spanKwic.getSpanContext().getBackwardTokens()) {
					prefixArrayJson.add(ti.getTerm());
				}
				
				tokenArray.add(spanKwic.getKeyword());
				
				ArrayNode postfixArrayJson = factory.arrayNode();
				postfixArraysJson.add(postfixArrayJson);
				
				for (TermInfo ti : spanKwic.getSpanContext().getForwardTokens()) {
					postfixArrayJson.add(ti.getTerm());
				}
			}
		}
		
		return kwicListJson.toString();
	}
}
