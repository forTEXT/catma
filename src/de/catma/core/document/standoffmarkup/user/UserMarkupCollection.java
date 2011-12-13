package de.catma.core.document.standoffmarkup.user;

import java.util.Collections;
import java.util.List;

import de.catma.core.tag.TagLibrary;

public class UserMarkupCollection {
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

	
}
