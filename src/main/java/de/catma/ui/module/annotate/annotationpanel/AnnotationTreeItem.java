package de.catma.ui.module.annotate.annotationpanel;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public interface AnnotationTreeItem extends Comparable<AnnotationTreeItem> {
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 100;
	
	public String getDetail();
	public default String getTagName() {return null;};
	public default String getTagPath() {return null;};
	public default String getAuthor() {return null;};
	public default String getCollectionName() {return null;};
	public default String getTagsetName() {return null;};
	public default String getAnnotationId() {return null;};
	public String getDescription();
	public default String getEditIcon() {return null;};
	public default String getDeleteIcon() {return null;};
	public default PropertyDefinition getPropertyDefinition() {return null;}; 

	default String shorten(String keyword, int maxLength) {
		if (keyword.length() <= maxLength) {
			return keyword;
		}
		
		return keyword.substring(0, maxLength/2) 
				+"["+HORIZONTAL_ELLIPSIS+"]"
				+ keyword.substring(keyword.length()-((maxLength/2)-2), keyword.length());
	}

	default void refreshDescription() {}
	TagDefinition getTag();
	TagsetDefinition getTagset();
	String getSortableAuthor();
	AnnotationCollection getCollection();
	String getSortableTagPath();
	
	int compareToByTag(AnnotationTreeItem o);
	int compareToByTagPath(AnnotationTreeItem o);
	int compareToByAuthor(AnnotationTreeItem o);
	int compareToByCollection(AnnotationTreeItem o);
	int compareToByTagset(AnnotationTreeItem o);

}
