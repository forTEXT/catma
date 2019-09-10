package de.catma.ui.module.annotate.annotationpanel;

public interface AnnotationTreeItem {
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 100;
	
	public String getDetail();
	public default String getTag() {return null;};
	public default String getAuthor() {return null;};
	public default String getCollection() {return null;};
	public default String getTagset() {return null;};
	public default String getAnnotationId() {return null;};
	public String getDescription();
	public default String getEditIcon() {return null;};
	public default String getDeleteIcon() {return null;};

	default String shorten(String keyword, int maxLength) {
		if (keyword.length() <= maxLength) {
			return keyword;
		}
		
		return keyword.substring(0, maxLength/2) 
				+"["+HORIZONTAL_ELLIPSIS+"]"
				+ keyword.substring(keyword.length()-((maxLength/2)-2), keyword.length());
	}



}
