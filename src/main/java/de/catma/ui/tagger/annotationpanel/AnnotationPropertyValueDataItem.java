package de.catma.ui.tagger.annotationpanel;

public class AnnotationPropertyValueDataItem implements AnnotationTreeItem {
	
	private String value;
	
	public AnnotationPropertyValueDataItem(String value) {
		super();
		this.value = value;
	}

	@Override
	public String getName() {
		return value; //intended
	}

	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return null;
	}

}
