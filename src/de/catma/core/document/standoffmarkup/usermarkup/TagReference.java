package de.catma.core.document.standoffmarkup.usermarkup;

import java.net.URI;
import java.net.URISyntaxException;

import de.catma.core.document.Range;
import de.catma.core.tag.Property;
import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
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

	public TagDefinition getTagDefinition() {
		return tagInstance.getTagDefinition();
	}
	
	public String getTagInstanceID() {
		return tagInstance.getID();
	}
	
	public Range getRange() {
		return range;
	}
	
	public String getColor() {
		return tagInstance.getSystemProperty(
			PropertyDefinition.SystemPropertyName.catma_displaycolor.name()).
				getPropertyValueList().getFirstValue();
	}
}
