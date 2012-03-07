package de.catma.core.document.standoffmarkup.usermarkup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibrary;

public class UserMarkupCollection {
	private String id;
	private String name;
	private TagLibrary tagLibrary;
	private List<TagReference> tagReferences;
	
	public UserMarkupCollection(TagLibrary tagLibrary,
			List<TagReference> tagReferences) {
		super();
		this.tagLibrary = tagLibrary;
		this.tagReferences = tagReferences;
	}
	
	public TagLibrary getTagLibrary() {
		return tagLibrary;
	}
	
	public List<TagReference> getTagReferences() {
		return Collections.unmodifiableList(tagReferences);
	}

	public List<TagReference> getTagReferences(TagDefinition tagDefinition) {
		List<TagReference> result = new ArrayList<TagReference>();
		for (TagReference tr : tagReferences) {
			if (tr.getTagDefinition().getID().equals(tagDefinition.getID())) {
				result.add(tr);
			}
		}
		
		return result;
	}

	@Override
	public String toString() {
		return name;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
