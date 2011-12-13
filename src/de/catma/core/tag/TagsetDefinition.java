package de.catma.core.tag;

import java.util.HashMap;
import java.util.Map;

public class TagsetDefinition implements Versionable {
	
	private String id;
	private String name;
	private Version version;
	private Map<String,TagDefinition> tagDefinitions;
	
	public TagsetDefinition(String id, String tagsetName, Version version) {
		this.id = id;
		this.name = tagsetName;
		this.version = version;
		this.tagDefinitions = new HashMap<String, TagDefinition>();
	}

	public Version getVersion() {
		return version;
	}
	
	@Override
	public String toString() {
		return "TAGSET_DEF["+name+",#"+id+","+version+"]";
	}

	public void addTagDefinition(TagDefinition tagDef) {
		tagDefinitions.put(tagDef.getID(),tagDef);
	}
	
	public String getID() {
		return id;
	}
	
	public boolean hasTagDefinition(String tagDefID) {
		return tagDefinitions.containsKey(tagDefID);
	}
	
	public TagDefinition getTagDefinition(String tagDefinitionID) {
		return tagDefinitions.get(tagDefinitionID);
	}
}
