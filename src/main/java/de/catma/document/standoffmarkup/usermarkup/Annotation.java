package de.catma.document.standoffmarkup.usermarkup;

import java.util.Collections;
import java.util.List;

import de.catma.tag.TagInstance;

public class Annotation {

	private final List<TagReference> tagReferences;
	private final TagInstance tagInstance;
	private final UserMarkupCollection userMarkupCollection;
	private final String tagPath;
	
	public Annotation(TagInstance tagInstance, List<TagReference> tagReferences,
			UserMarkupCollection userMarkupCollection, String tagPath) {
		this.tagInstance = tagInstance;
		this.tagReferences = tagReferences;
		this.userMarkupCollection = userMarkupCollection;
		this.tagPath = tagPath;
	}

	public TagInstance getTagInstance() {
		return tagInstance;
	}

	public UserMarkupCollection getUserMarkupCollection() {
		return userMarkupCollection;
	}

	public String getTagPath() {
		return tagPath;
	}
	
	public List<TagReference> getTagReferences() {
		return Collections.unmodifiableList(tagReferences);
	}
}
