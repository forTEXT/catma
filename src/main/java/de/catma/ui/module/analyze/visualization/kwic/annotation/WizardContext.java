package de.catma.ui.module.analyze.visualization.kwic.annotation;

import java.util.HashMap;

public class WizardContext {
	private HashMap<Enum<?>, Object> values;
	
	public WizardContext() {
		this.values = new HashMap<Enum<?>, Object>();
	}
	
	public Object get(Enum<?> key) {
		return this.values.get(key);
	}
	
	public Object put(Enum<?> key, Object value) {
		return values.put(key, value);
	}
}
