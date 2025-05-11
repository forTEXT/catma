package de.catma.ui.module.tags;

import com.vaadin.data.provider.TreeDataProvider;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

interface TagsetTreeItem extends Comparable<TagsetTreeItem> {
	public String getColor();
	public String getName();
	public String getTagsetName();
	public String getPropertySummary();
	public String getRemoveIcon();
	
	public void handleRemovalRequest(TagsView tagsView);
	
	public default void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {}
	public default String generateStyle() { return null; }
	public default String getPropertyValue() { return null; }
	boolean isEditable();
	
	public default void setTagsetExpanded(boolean expanded) {};
	
	public default String getResponsibleUser() { return null; }

	TagDefinition getTag();
	PropertyDefinition getPropertyDefinition();
	TagsetDefinition getTagset();

}
