package de.catma.core.tag;

import java.util.Set;

public class TagsetDefinition implements Versionable {
	private String id;
	private String name;
	private Version version;
	private Set<TagDefinition> tagDefinitions;
	
	public TagsetDefinition(String id, String tagsetName, Version version) {
		this.id = id;
		this.name = tagsetName;
		this.version = version;
	}

	public Version getVersion() {
		return version;
	}
	
	@Override
	public String toString() {
		return "TAGSET["+name+",#"+id+","+version+"]";
	}
}
