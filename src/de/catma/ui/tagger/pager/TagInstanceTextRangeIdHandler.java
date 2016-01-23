package de.catma.ui.tagger.pager;

import java.util.HashMap;
import java.util.Map;

public class TagInstanceTextRangeIdHandler {
	private Map<String, Integer> tagInstanceIdToRangeIdMapping;
	
	public TagInstanceTextRangeIdHandler() {
		this.tagInstanceIdToRangeIdMapping = new HashMap<>();
	}
	
	public Integer getTextRangeIncrement(String tagInstanceID) {
		Integer textRangeId = tagInstanceIdToRangeIdMapping.get(tagInstanceID);
		if (textRangeId == null) {
			textRangeId = 0;
		}
		
		textRangeId++;
		tagInstanceIdToRangeIdMapping.put(tagInstanceID, textRangeId);
		
		return textRangeId;
	}
}
