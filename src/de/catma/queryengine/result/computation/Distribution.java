/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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
package de.catma.queryengine.result.computation;

import java.util.ArrayList;
import java.util.List;


public class Distribution {
	
	private String id;
	private String label;
	private List<XYValues<Integer, Integer>> xySeries;
	private double segmentSize;
	private int segmentSizeInPercent;
	
	public Distribution(String id, String label,
			double segmentSize,
			int segmentSizeInPercent) {
		super();
		this.id = id;
		this.label = label;
		this.segmentSize = segmentSize;
		this.segmentSizeInPercent = segmentSizeInPercent;
		this.xySeries = new ArrayList<XYValues<Integer,Integer>>();
	}

	public String getLabel() {
		return label;
	}
	
	public String getId() {
		return id;
	}
	
	public double getSegmentSize() {
		return segmentSize;
	}

	public int getSegmentSizeInPercent() {
		return segmentSizeInPercent;
	}
	
	public List<XYValues<Integer, Integer>> getXySeries() {
		return xySeries;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Distribution other = (Distribution) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	public void add(XYValues<Integer, Integer> xyValues) {
		xySeries.add(xyValues);
	}
	
	
}
