package de.catma.repository.git.graph.lazy;

import java.util.function.Predicate;

import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.util.Pair;


class PropertyNameFilter implements Predicate<Pair<Property, TagDefinition>> {

	private String propertyNameRegex;

	public PropertyNameFilter(String propertyName) {
		this.propertyNameRegex = propertyName==null?null:SQLWildcard2RegexConverter.convert(propertyName);
	}

	@Override
	public boolean test(Pair<Property, TagDefinition> propertyTagPair) {
		
		if (propertyNameRegex == null) {
			return true;
		}
		
		String propertyName = 
				propertyTagPair.getSecond().getPropertyDefinitionByUuid(
						propertyTagPair.getFirst().getPropertyDefinitionId())
				.getName();
		
		return propertyName.matches(propertyNameRegex);
	}
	
	public boolean testPropertyName(String propertyName) {
		if (propertyNameRegex == null) {
			return true;
		}
		
		return propertyName.matches(propertyNameRegex);
	}

}
