package de.catma.core.tag;

import java.util.Collections;
import java.util.List;

public class PropertyValueList {

	private List<String> values;

	public PropertyValueList(List<String> values) {
		super();
		this.values = values;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<values.size(); i++) {
			if (i>1) {
				sb.append(",");
			}
			sb.append(values.get(i));
		}
		
		return sb.toString();
	}

	public String getFirstValue() {
		if (values.size() > 0) {
			return values.get(0);
		}
		return null;
	}
	
	public List<String> getValues() {
		return Collections.unmodifiableList(values);
	}
	
}
