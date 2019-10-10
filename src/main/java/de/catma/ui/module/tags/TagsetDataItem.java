package de.catma.ui.module.tags;

import java.util.Collections;

import com.vaadin.icons.VaadinIcons;

import de.catma.tag.TagsetDefinition;

class TagsetDataItem implements TagsetTreeItem {
	
	private TagsetDefinition tagset;
	private boolean editable;

	public TagsetDataItem(TagsetDefinition tagset) {
		this(tagset, false);
	}
	
	public TagsetDataItem(TagsetDefinition tagset, boolean editable) {
		super();
		this.tagset = tagset;
		this.editable = editable;
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
	public String getPropertySummary() {
		return null; // no properties for Tagsets
	}
	
	@Override
	public String toString() {
		return tagset.getName();
	}

	@Override
	public String getRemoveIcon() {
		if (editable) {
			return VaadinIcons.TRASH.getHtml();
		}
		return VaadinIcons.LOCK.getHtml();
	}
	
	@Override
	public boolean isEditable() {
		return editable;
	}
	
	@Override
	public void handleRemovalRequest(TagsView tagsView) {
		if (editable) {
			tagsView.deleteTagsets(Collections.singleton(tagset));
		}
	}
}
