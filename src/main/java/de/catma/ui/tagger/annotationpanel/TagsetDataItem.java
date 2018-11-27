package de.catma.ui.tagger.annotationpanel;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;

import de.catma.tag.TagsetDefinition;

public class TagsetDataItem implements TagTreeItem {
	
	private TagsetDefinition tagset;
	

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
		return tagset.getName();
	}

	public TagsetDefinition getTagset() {
		return tagset;
	}
	
	@Override
	public String getVisibilityIcon() {
		return VaadinIcons.EYE.getHtml();
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
	
	
}
