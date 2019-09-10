package de.catma.ui.module.annotate.annotationpanel;

public class AnnotationPropertyValueDataItem implements AnnotationTreeItem {
	
	private String value;
	
	public AnnotationPropertyValueDataItem(String value) {
		super();
		this.value = value;
	}

	@Override
	public String getDetail() {
		if (value.length() > MAX_VALUE_LENGTH) {
			return shorten(value, MAX_VALUE_LENGTH);
		}
		return value;
	}



	@Override
	public String getDescription() {
		return value;
	}

}
