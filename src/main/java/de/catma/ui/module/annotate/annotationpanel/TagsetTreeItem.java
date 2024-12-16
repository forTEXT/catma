package de.catma.ui.module.annotate.annotationpanel;

import java.util.List;

import com.vaadin.data.provider.TreeDataProvider;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

interface TagsetTreeItem extends Comparable<TagsetTreeItem> {
	public String getId();
	public String getColor();
	public String getName();
	public String getTagsetName();
	public String getVisibilityIcon();
	public String getPropertySummary();
	public boolean isVisible();
	public void setVisible(boolean visible);
	public List<TagReference> getTagReferences(List<AnnotationCollection> collections);
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> treeDataProvider, boolean visible, boolean explicit);
	
	public default void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {}
	public default String generateStyle() { return null; }
	public default String getPropertyValue() { return null; }

	public default void setTagsetExpanded(boolean expanded) {};
	
	public TagsetDefinition getTagset();
	public TagDefinition getTag();
	public PropertyDefinition getPropertyDefinition();

}
