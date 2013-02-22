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
import java.util.Collections;
import java.util.List;



/**
 * @author marco.petris@web.de
 *
 */
public class ClientTagInstance {
	
	public static enum SerializationField {
		tagDefinitionID,
		instanceID,
		color,
		ranges,
		startPos,
		endPos,
		;
	}
	
	final private String tagDefinitionID;
	final private String color;
	final private String instanceID;
	final private List<TextRange> ranges;

	public ClientTagInstance(
			String tagDefinitionID, String instanceID, 
			String color, List<TextRange> ranges) {
		super();
		this.tagDefinitionID = tagDefinitionID;
		this.instanceID = instanceID;
		this.color = color;
		this.ranges = ranges;
	}
	
	public ClientTagInstance(
			ClientTagInstance tagInstanceToCopy, int base) {
		this(tagInstanceToCopy.tagDefinitionID,
				tagInstanceToCopy.instanceID, 
				tagInstanceToCopy.color, new ArrayList<TextRange>());
		for (TextRange tr : tagInstanceToCopy.getRanges()) {
			ranges.add(new TextRange(tr.getStartPos()+base, tr.getEndPos()+base));
		}
	}
	
	public ClientTagInstance(
			ClientTagInstance tagInstanceToCopy, int base, 
			int pageStart, int pageEnd) {
		this(tagInstanceToCopy.tagDefinitionID,
				tagInstanceToCopy.instanceID, 
				tagInstanceToCopy.color, new ArrayList<TextRange>());
		
		TextRange pageRange = new TextRange(pageStart, pageEnd);
		
		for (TextRange tr : tagInstanceToCopy.getRanges()) {
			
			TextRange overlappingRange = pageRange.getOverlappingRange(tr);
		
			if (overlappingRange != null) {
				ranges.add(
					new TextRange(overlappingRange.getStartPos()+base,
							overlappingRange.getEndPos()+base));
			}
		}
	}

	public String getTagDefinitionID() {
		return tagDefinitionID;
	}
	
	public String getInstanceID() {
		return instanceID;
	}
	
	public String getColor() {
		return color;
	}
	
	public List<TextRange> getRanges() {
		return Collections.unmodifiableList(ranges);
	}
	
	@Override
	public String toString() {
		return "#" + instanceID + " " 
				+ ((getRanges().size()>0)? getRanges().get(0) :"");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instanceID == null) ? 0 : instanceID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientTagInstance other = (ClientTagInstance) obj;
		if (instanceID == null) {
			if (other.instanceID != null)
				return false;
		} else if (!instanceID.equals(other.instanceID))
			return false;
		return true;
	}
	
	public void addRanges(List<TextRange> moreRanges) {
		for (TextRange tr : moreRanges) {
			if (!ranges.contains(tr)) {
				this.ranges.add(tr);
			}
		}
	}

	
}
