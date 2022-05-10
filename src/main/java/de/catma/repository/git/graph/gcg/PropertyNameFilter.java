package de.catma.repository.git.graph.gcg;

import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;


class PropertyNameFilter implements Predicate<Traverser<Vertex>> {

	private String propertyNameRegex;

	public PropertyNameFilter(String propertyName) {
		this.propertyNameRegex = propertyName==null?null:SQLWildcard2RegexConverter.convert(propertyName);
	}

	@Override
	public boolean test(Traverser<Vertex> t) {
		
		if (propertyNameRegex == null) {
			return true;
		}
		
		String propertyName = (String)t.get().property("name").value();
		
		return propertyName.matches(propertyNameRegex);
	}
	
	public boolean testPropertyName(String propertyName) {
		if (propertyNameRegex == null) {
			return true;
		}
		
		return propertyName.matches(propertyNameRegex);
	}

}
