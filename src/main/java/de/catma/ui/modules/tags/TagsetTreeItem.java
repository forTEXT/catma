package de.catma.ui.modules.tags;

import java.util.List;

import com.vaadin.data.provider.TreeDataProvider;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;

interface TagsetTreeItem {
	public String getColor();
	public String getName();
	public String getTagsetName();
	public String getVisibilityIcon();
	public String getPropertySummary();
	public boolean isVisible();
	public void setVisible(boolean visible);
	public List<TagReference> getTagReferences(List<UserMarkupCollection> collections);
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> treeDataProvider, boolean visible, boolean explicit);
	
	public default void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {}
	public default String generateStyle() { return null; }
	public default String getPropertyValue() { return null; }

}
