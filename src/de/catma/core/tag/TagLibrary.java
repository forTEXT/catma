package de.catma.core.tag;

import java.util.List;

public class TagLibrary {

	private String name;
	private List<TagsetDefinition> tagsetDefinitions;
	
	public TagLibrary(String name) {
		super();
		this.name = name;
	}

	public void add(TagsetDefinition tagsetDefinition) {
		tagsetDefinitions.add(tagsetDefinition);
	}
	
}
