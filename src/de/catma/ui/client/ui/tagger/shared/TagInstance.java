/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
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
package de.catma.ui.client.ui.tagger.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * @author marco.petris@web.de
 *
 */
public class TagInstance {
	
	private static enum SerializationField {
		instanceID,
		color,
		startPos,
		endPos,
		;
	}
	
	private String color;
	private String instanceID;
	private List<TextRange> ranges;

	public TagInstance(String instanceID, String color, List<TextRange> ranges) {
		super();
		this.instanceID = instanceID;
		this.color = color;
		this.ranges = ranges;
	}
	
	public TagInstance(Map<String,Object> serializedEvent) {
		this.instanceID = 
				(String)serializedEvent.get(SerializationField.instanceID.name());
		this.color = 
				(String)serializedEvent.get(SerializationField.color.name());

		ranges = new ArrayList<TextRange>();
		int i=0;
		while(serializedEvent.containsKey(SerializationField.startPos.name()+i)){
			ranges.add(
				new TextRange(
					Integer.valueOf(
						(String)serializedEvent.get(
								SerializationField.startPos.name()+i)),
					Integer.valueOf(
						(String)serializedEvent.get(
								SerializationField.endPos.name()+i))));
			
			i++;
		}
	}
	
	public TagInstance(TagInstance tagInstanceToCopy, int base) {
		this(tagInstanceToCopy.instanceID, 
				tagInstanceToCopy.color, new ArrayList<TextRange>());
		for (TextRange tr : tagInstanceToCopy.getRanges()) {
			ranges.add(new TextRange(tr.getStartPos()+base, tr.getEndPos()+base));
		}
	}

	public Map<String,Object> toMap() {
		Map<String, Object> result =
				new HashMap<String, Object>();
		
		int i=0;
		result.put(SerializationField.instanceID.name(), instanceID);
		result.put(SerializationField.color.name(), color);

		for (TextRange tr : ranges) {
			result.put(
				SerializationField.startPos.name()+i, 
				String.valueOf(tr.getStartPos()));
			result.put(
				SerializationField.endPos.name()+i, 
				String.valueOf(tr.getEndPos()));
			i++;
		}
		
		return result;
	}

	public String getInstanceID() {
		return instanceID;
	}
	
	public String getColor() {
		return color;
	}
	
	public List<TextRange> getRanges() {
		return ranges;
	}
	
	@Override
	public String toString() {
		return "#" + instanceID + " " 
				+ ((getRanges().size()>0)? getRanges().get(0) :"");
	}
}
