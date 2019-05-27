package de.catma.ui.tagger.annotationpanel;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagsetDefinition;

public class TagsetDataItem implements TagsetTreeItem {
	
	private TagsetDefinition tagset;
	private boolean visible;

	public TagsetDataItem(TagsetDefinition tagset) {
		super();
		this.tagset = tagset;
	}

	@Override
	public String getColor() {
		return getTagsetName();
	}

	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public String getTagsetName() {
		return "<span class=\"annotate-panel-tagsetname\">"+tagset.getName()+"</span>";
	}

	public TagsetDefinition getTagset() {
		return tagset;
	}
	
	@Override
	public String getVisibilityIcon() {
		return visible?VaadinIcons.EYE.getHtml():VaadinIcons.EYE_SLASH.getHtml();
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tagset == null) ? 0 : tagset.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagsetDataItem other = (TagsetDataItem) obj;
		if (tagset == null) {
			if (other.tagset != null)
				return false;
		} else if (!tagset.equals(other.tagset))
			return false;
		return true;
	}
	
	
	@Override
	public boolean isVisible() {
		return visible;
	}
	
	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	@Override
	public List<TagReference> getTagReferences(List<UserMarkupCollection> collections) {
		List<TagReference> result = new ArrayList<>();
		
		for (UserMarkupCollection collection : collections) {
			result.addAll(collection.getTagReferences(tagset));
		}
		
		return result;
	}
	
	@Override
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> dataProvider, boolean visible, boolean explicit) {
		for (TagsetTreeItem tagTreeItem : dataProvider.getTreeData().getChildren(this)) {
			setChildrenVisible(tagTreeItem, visible, dataProvider);
		}
	}

	private void setChildrenVisible(TagsetTreeItem tagTreeItem, boolean visible, TreeDataProvider<TagsetTreeItem> dataProvider) {
		tagTreeItem.setVisible(visible);
		dataProvider.refreshItem(tagTreeItem);
		for (TagsetTreeItem tagTreeChildItem : dataProvider.getTreeData().getChildren(tagTreeItem)) {
			setChildrenVisible(tagTreeChildItem, visible, dataProvider);
		}		
	}
	
	@Override
	public String getPropertySummary() {
		return null; // no properties for Tagsets
	}
	
	@Override
	public String toString() {
		return tagset.getName();
	}

}
