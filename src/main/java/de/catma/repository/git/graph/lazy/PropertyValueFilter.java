package de.catma.repository.git.graph.lazy;

import java.util.List;
import java.util.function.Predicate;

import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;
import de.catma.tag.Property;

public class PropertyValueFilter implements Predicate<Property> {

	private String propertyValueRegex;


	public PropertyValueFilter(String propertyValue) {
		this.propertyValueRegex = propertyValue==null?null:SQLWildcard2RegexConverter.convert(propertyValue);
	}

	
	@Override
	public boolean test(Property property) {
		if (propertyValueRegex == null) {
			return true;
		}
		
		List<String> values = property.getPropertyValueList();
		
		for (String value : values) {
			if (value.matches(propertyValueRegex)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean testValue(String value) {
		if (propertyValueRegex == null) {
			return true;
		}
		return value.matches(propertyValueRegex);
	}
}
