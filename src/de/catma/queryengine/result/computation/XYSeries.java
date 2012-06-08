package de.catma.queryengine.result.computation;

import java.util.ArrayList;
import java.util.List;

import de.catma.util.Pair;

public class XYSeries<X,Y> {

	private Object key;
	private List<Pair<X,Y>> xySeries;
	
	public XYSeries(Object key) {
		this.key = key;
		xySeries = new ArrayList<Pair<X,Y>>();
	}
	
	public void add(X x, Y y) {
		xySeries.add(new Pair<X, Y>(x,y));
	}
	
	public Object getKey() {
		return key;
	}
}
