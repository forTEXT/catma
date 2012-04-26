package de.catma.indexer.elasticsearch;

import java.lang.reflect.Field;

public class ESKey {

	Field[] fields;
	Object instance;

	public ESKey(Object instance, String... fields) throws NoSuchFieldException {
		for (int i = 0; i < fields.length; i++) {
			this.fields[i] = instance.getClass().getField(fields[i]);
		}
	}

	public boolean isNotNull() throws IllegalAccessException {
		for (Field f : fields) {
			if (f.get(this.instance) == null) {
				return false;
			}
		}
		return true;
	}
}
