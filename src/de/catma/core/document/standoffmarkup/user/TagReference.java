package de.catma.core.document.standoffmarkup.user;

import java.net.URI;
import java.net.URISyntaxException;

import de.catma.core.document.Range;
import de.catma.core.tag.TagInstance;
import de.catma.core.tag.TargetText;

public class TagReference {

	private TagInstance tagInstance;
	private Range range;
	private URI target;
	private TargetText targetText;

	public TagReference(TagInstance tagInstance, String uri, Range range) 
			throws URISyntaxException {
		this.tagInstance = tagInstance;
		this.target = new URI(uri);
		this.range = range;
	}
	
	@Override
	public String toString() {
		return tagInstance + "@" + target + "#" + range;
	}
}
