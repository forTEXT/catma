package de.catma.ui.client.ui.tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CTagsetDefinition implements CVersionable, Iterable<CTagDefinition> {
	
	private String id;
	private String name;
	private CVersion version;
	private Map<String,CTagDefinition> tagDefinitions;
	private Map<String,CTagDefinition> parentChildTagDefRelationships;
	
	public CTagsetDefinition(String id, String tagsetName, CVersion version) {
		this.id = id;
		this.name = tagsetName;
		this.version = version;
		this.tagDefinitions = new HashMap<String, CTagDefinition>();
	}

	public CVersion getVersion() {
		return version;
	}
	
	@Override
	public String toString() {
		return "TAGSET_DEF["+name+",#"+id+","+version+"]";
	}

	public void addTagDefinition(CTagDefinition tagDef) {
		tagDefinitions.put(tagDef.getID(),tagDef);
	}
	
	public String getID() {
		return id;
	}
	
	public boolean hasTagDefinition(String tagDefID) {
		return tagDefinitions.containsKey(tagDefID);
	}

	public boolean hasTagDefinition(CTagDefinition tagDefintion) {
		return (tagDefinitions.containsKey(tagDefintion.getID()) 
				&& (tagDefinitions.get(
						tagDefintion.getID()).equals(tagDefintion))); 
	}
	
	public CTagDefinition getTagDefinition(String tagDefinitionID) {
		return tagDefinitions.get(tagDefinitionID);
	}
	
	public Iterator<CTagDefinition> iterator() {
		return Collections.unmodifiableCollection(tagDefinitions.values()).iterator();
	}
	
	public String getName() {
		return name;
	}
}
