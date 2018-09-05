package de.catma.repository.db.jooq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BindFactory {

	private ArrayList<Object> args;
	
	public BindFactory(int size) {
		args = new ArrayList<Object>(size);
	}
	
	public void add(Object... values) {
		for (Object value : values) {
			args.add(value);
		}
	}
	
	public void addAll(List<?> values) {
		args.addAll(values);
	}
	
	public Object[] toArray() {
		return args.toArray();
	}
}
