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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.catma.queryengine.result.QueryResultRow;


public class Distribution {
	
	private String id;
	private String label;
	private Map<String, XYValues<Integer, Integer, QueryResultRow>> xySeries;
	private double segmentSize;
	private double segmentSizeInPercent;
	
	public Distribution(String id, String label,
			double segmentSize,
			double segmentSizeInPercent) {
		super();
		this.id = id;
		this.label = label;
		this.segmentSize = segmentSize;
		this.segmentSizeInPercent = segmentSizeInPercent;
		this.xySeries = new HashMap<String, XYValues<Integer,Integer, QueryResultRow>>();
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

	public double getSegmentSizeInPercent() {
		return segmentSizeInPercent;
	}
	
	public List<XYValues<Integer, Integer, QueryResultRow>> getXySeries() {
		return new ArrayList<XYValues<Integer,Integer,QueryResultRow>>(this.xySeries.values());
	}
	
	public void add(XYValues<Integer, Integer, QueryResultRow> xyValues) {
		this.xySeries.put(xyValues.getKey().toString(), xyValues);
	}

	public List<QueryResultRow> getQueryResultRows(String key, int x) {
		return xySeries.get(key).getData(x);
	}


	
}
