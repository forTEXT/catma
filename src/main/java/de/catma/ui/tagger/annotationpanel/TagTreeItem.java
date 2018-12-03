package de.catma.ui.tagger.annotationpanel;

public interface TagTreeItem {
	public static enum Property {
		color,
		name,
	}
	
	public String getColor();
	public String getName();
	public String getTagsetName();
	public String getVisibilityIcon();
	

}
