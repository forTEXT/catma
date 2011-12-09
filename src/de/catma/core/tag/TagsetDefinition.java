package de.catma.core.tag;

import java.util.ArrayList;
import java.util.List;

public class TagsetDefinition implements Versionable {
	
	private String id;
	private String name;
	private Version version;
	private List<TagDefinition> tagDefinitions;
	
	public TagsetDefinition(String id, String tagsetName, Version version) {
		this.id = id;
		this.name = tagsetName;
		this.version = version;
		this.tagDefinitions = new ArrayList<TagDefinition>();
	}

	public Version getVersion() {
		return version;
	}
	
	@Override
	public String toString() {
		return "TAGSET_DEF["+name+",#"+id+","+version+"]";
	}

	public void add(TagDefinition tagDef) {
		tagDefinitions.add(tagDef);
	}
}
