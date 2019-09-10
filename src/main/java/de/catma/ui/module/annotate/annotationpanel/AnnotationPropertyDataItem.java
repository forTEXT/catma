package de.catma.ui.module.annotate.annotationpanel;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.catma.tag.Property;

public class AnnotationPropertyDataItem implements AnnotationTreeItem {
	
	private Property property;
	private Supplier<String> propertyNameProvider;
	
	public AnnotationPropertyDataItem(Property property, Supplier<String> propertyNameProvider) {
		super();
		this.property = property;
		this.propertyNameProvider = propertyNameProvider;
	}

	@Override
	public String getDetail() {
		return propertyNameProvider.get();
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
