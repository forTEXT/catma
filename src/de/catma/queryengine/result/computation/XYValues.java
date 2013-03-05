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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class XYValues<X,Y> implements Iterable<Map.Entry<X,Y>> {
	
	private Object key;
	private Map<X,Y> xySeries;
	
	public XYValues(Object key) {
		this.key = key;
		xySeries = new LinkedHashMap<X,Y>();
	}
	
	public void set(X x, Y y) {
		xySeries.put(x,y);
	}

	public Object getKey() {
		return key;
	}
	
	public Y get(X x) {
		return xySeries.get(x);
	}
	
	public Iterator<Entry<X, Y>> iterator() {
		return xySeries.entrySet().iterator();
	}
	
	@Override
	public String toString() {
		return key + " " + Arrays.toString(xySeries.entrySet().toArray());
	}
}
