package de.catma.core.tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TagLibrary implements Iterable<TagsetDefinition> {

	private String name;
	private Map<String,TagsetDefinition> tagsetDefinitions;
	
	public TagLibrary(String name) {
		super();
		this.name = name;
		tagsetDefinitions = new HashMap<String, TagsetDefinition>();
	}

	public void add(TagsetDefinition tagsetDefinition) {
		tagsetDefinitions.put(tagsetDefinition.getID(),tagsetDefinition);
	}

	public TagDefinition getTagDefinition(String tagDefinitionID) {
		for(TagsetDefinition tagsetDefiniton : tagsetDefinitions.values()) {
			if (tagsetDefiniton.hasTagDefinition(tagDefinitionID)) {
				return tagsetDefiniton.getTagDefinition(tagDefinitionID);
			}
		}
		return null;
	}
	
	public Iterator<TagsetDefinition> iterator() {
		return Collections.unmodifiableCollection(tagsetDefinitions.values()).iterator();
	}

	public TagsetDefinition getTagsetDefintion(String tagsetDefinitionID) {
		return tagsetDefinitions.get(tagsetDefinitionID);
	}
	
	public String getName() {
		return name;
	}
}
