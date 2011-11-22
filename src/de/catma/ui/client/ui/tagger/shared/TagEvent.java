package de.catma.ui.client.ui.tagger.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.catma.ui.client.ui.tagger.TextRange;


public class TagEvent {
	
	private String tag;
	private List<TextRange> ranges;

	public TagEvent(String tag, List<TextRange> ranges) {
		super();
		this.tag = tag;
		this.ranges = ranges;
	}
	
	public TagEvent(Map<String,Object> serializedEvent) {
		this.tag = (String)serializedEvent.get("tag");
		ranges = new ArrayList<TextRange>();
		int i=0;
		while(serializedEvent.containsKey("startPos"+i)){
			ranges.add(
					new TextRange(
							(Integer)serializedEvent.get("startPos"+i),
							(Integer)serializedEvent.get("endPos"+i)));
			
			i++;
		}
	}
	
	public Map<String,Object> toMap() {
		Map<String, Object> result =
				new HashMap<String, Object>();
		
		int i=0;
		result.put("tag", tag);
		for (TextRange tr : ranges) {
			result.put("startPos"+i, tr.getStartPos());
			result.put("endPos"+i, tr.getEndPos());
			i++;
		}
		
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(tag);
		
		for (TextRange tr : ranges) {
			builder.append(tr.toString());
		}
		
		return builder.toString(); 
	}

	public String getTag() {
		return tag;
	}
}
