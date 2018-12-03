package de.catma.ui.tagger.annotationpanel;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;

import de.catma.tag.TagDefinition;
import de.catma.util.ColorConverter;

public class TagDataItem implements TagTreeItem {
	
	private TagDefinition tag;
	
	public TagDataItem(TagDefinition tag) {
		super();
		this.tag = tag;
	}

	@Override
	public String getColor() {
		String htmlColor = "#"+ColorConverter.toHex(tag.getColor());
		return "<div class=\"annotate-tag-tree-item\" style=\"background-color:"+htmlColor+"\">&nbsp;</div>";
	}

	@Override
	public String getName() {
		return tag.getName();
	}

	@Override
	public String getTagsetName() {
		return "";
	}
	
	public TagDefinition getTag() {
		return tag;
	}
	
	@Override
	public String getVisibilityIcon() {
		return VaadinIcons.EYE_SLASH.getHtml();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
		TagDataItem other = (TagDataItem) obj;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return tag.getName();
	}
	
}
