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
