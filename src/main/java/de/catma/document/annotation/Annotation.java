package de.catma.document.annotation;

import java.util.Collections;
import java.util.List;

import de.catma.tag.TagInstance;

public class Annotation {

	private final List<TagReference> tagReferences;
	private final TagInstance tagInstance;
	private final AnnotationCollection userMarkupCollection;
	private final String tagPath;
	
	public Annotation(TagInstance tagInstance, List<TagReference> tagReferences,
			AnnotationCollection userMarkupCollection, String tagPath) {
		this.tagInstance = tagInstance;
		this.tagReferences = tagReferences;
		this.userMarkupCollection = userMarkupCollection;
		this.tagPath = tagPath;
	}

	public TagInstance getTagInstance() {
		return tagInstance;
	}

	public AnnotationCollection getUserMarkupCollection() {
		return userMarkupCollection;
	}

	public String getTagPath() {
		return tagPath;
	}
	
	public List<TagReference> getTagReferences() {
		return Collections.unmodifiableList(tagReferences);
	}
}
