package de.catma.ui.tagger.annotationpanel;

import de.catma.tag.Property;

public class AnnotationPropertyDataItem implements AnnotationTreeItem {
	
	private Property property;
	
	public AnnotationPropertyDataItem(Property property) {
		super();
		this.property = property;
	}

	@Override
	public String getName() {
		return property.getName();
	}

	@Override
	public String getValue() {
		if (property.getPropertyValueList().size()==1) {
			return property.getFirstValue();
		}
		return null;
	}

}
