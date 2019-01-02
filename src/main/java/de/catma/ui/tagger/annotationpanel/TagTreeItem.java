package de.catma.ui.tagger.annotationpanel;

import java.util.List;

import com.vaadin.data.provider.TreeDataProvider;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;

public interface TagTreeItem {
	public static enum Property {
		color,
		name,
	}
	
	public String getColor();
	public String getName();
	public String getTagsetName();
	public String getVisibilityIcon();
	public boolean isVisible();
	public void setVisible(boolean visible);
	public List<TagReference> getTagReferences(List<UserMarkupCollection> collections);
	public void setChildrenVisible(TreeDataProvider<TagTreeItem> treeDataProvider, boolean visible, boolean explicit);
	

}
