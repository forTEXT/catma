package de.catma.ui.client.ui.tagger.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;

import de.catma.ui.client.ui.tagger.TextRange;


public class TagInstance {
	
	private static enum SerializationField {
		tag,
		instanceID,
		startPos,
		endPos,
		;
	}
	
	private String tag;
	private String instanceID;
	private List<TextRange> ranges;

	public TagInstance(String tag, String instanceID, List<TextRange> ranges) {
		super();
		this.tag = tag;
		this.instanceID = instanceID;
		this.ranges = ranges;
	}
	
	public TagInstance(Map<String,Object> serializedEvent) {
		this.tag = (String)serializedEvent.get(SerializationField.tag.name());
		this.instanceID = (String)serializedEvent.get(SerializationField.instanceID.name());
		ranges = new ArrayList<TextRange>();
		int i=0;
		while(serializedEvent.containsKey(SerializationField.startPos.name()+i)){
			ranges.add(
					new TextRange(
							(Integer)serializedEvent.get(SerializationField.startPos.name()+i),
							(Integer)serializedEvent.get(SerializationField.endPos.name()+i)));
			
			i++;
		}
	}
	
	public TagInstance(String serializedEvent) {
		
		int rangesStart = serializedEvent.indexOf("[");
		String rangeValues = serializedEvent.substring(rangesStart);
		RegExp idPattern = RegExp.compile("#");
		SplitResult nameAndID = idPattern.split(serializedEvent.substring(0, rangesStart));
	
		tag = nameAndID.get(0);
		instanceID = nameAndID.get(1);
		
		ranges = new ArrayList<TextRange>();
		
		RegExp rangesPattern = RegExp.compile("\\[(\\d+),(\\d+)\\]","g");
		
		MatchResult matchResult = null;
		
		while ((matchResult = rangesPattern.exec(
				rangeValues.substring(rangesPattern.getLastIndex()))) != null) {
			ranges.add(
					new TextRange(
							Integer.valueOf(matchResult.getGroup(1)),
							Integer.valueOf(matchResult.getGroup(2))));
		}
	}

	public Map<String,Object> toMap() {
		Map<String, Object> result =
				new HashMap<String, Object>();
		
		int i=0;
		result.put(SerializationField.tag.name(), tag);
		result.put(SerializationField.instanceID.name(), instanceID);
		for (TextRange tr : ranges) {
			result.put(SerializationField.startPos.name()+i, tr.getStartPos());
			result.put(SerializationField.endPos.name()+i, tr.getEndPos());
			i++;
		}
		
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(tag);
		builder.append("#");
		builder.append(instanceID);
		
		for (TextRange tr : ranges) {
			builder.append(tr.toString());
		}
		
		return builder.toString(); 
	}

	public String getTag() {
		return tag;
	}
	
	public String getInstanceID() {
		return instanceID;
	}
	
	public List<TextRange> getRanges() {
		return ranges;
	}
}
