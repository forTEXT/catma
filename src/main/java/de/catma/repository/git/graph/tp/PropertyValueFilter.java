package de.catma.repository.git.graph.tp;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;

public class PropertyValueFilter implements Predicate<Traverser<Vertex>> {

	private String propertyValueRegex;


	public PropertyValueFilter(String propertyValue) {
		this.propertyValueRegex = propertyValue==null?null:SQLWildcard2RegexConverter.convert(propertyValue);
	}

	
	@Override
	public boolean test(Traverser<Vertex> t) {
		if (propertyValueRegex == null) {
			return true;
		}
		
		VertexProperty<Object> valuesProperty = t.get().property("values");
		
		@SuppressWarnings("unchecked")
		List<String> values = 
			(List<String>) valuesProperty.orElse(Collections.<String>emptyList());
		
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
