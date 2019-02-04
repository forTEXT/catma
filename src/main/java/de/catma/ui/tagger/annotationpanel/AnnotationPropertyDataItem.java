package de.catma.ui.tagger.annotationpanel;

import java.util.stream.Collectors;

import de.catma.tag.Property;

public class AnnotationPropertyDataItem implements AnnotationTreeItem {
	
	private Property property;
	
	public AnnotationPropertyDataItem(Property property) {
		super();
		this.property = property;
	}

	@Override
	public String getDetail() {
		return property.getName();
	}
	
	@Override
	public String getTag() {
		return null;
	}

	@Override
	public String getDescription() {
		return property.getPropertyValueList().stream().collect(Collectors.joining(","));
	}

	
}
