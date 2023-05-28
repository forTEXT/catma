package de.catma.ui.module.tags;

import java.util.Collections;
import java.util.stream.Collectors;

import com.vaadin.icons.VaadinIcons;

import de.catma.tag.TagDefinition;
import de.catma.ui.util.Cleaner;
import de.catma.util.ColorConverter;

class TagDataItem implements TagsetTreeItem {
	
	private TagDefinition tag;
	private boolean propertiesExpanded;
	private boolean editable;
	
	public TagDataItem(TagDefinition tag) {
		this(tag, false);
	}

	public TagDataItem(TagDefinition tag, boolean editable) {
		super();
		this.tag = tag;
		this.editable = editable;
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
	
	@Override
	public String getPropertySummary() {
		if (tag.getUserDefinedPropertyDefinitions().isEmpty()) {
			return null;
		}
		
		StringBuilder propertySummary = new StringBuilder();
		propertySummary.append(
			(propertiesExpanded?VaadinIcons.CARET_DOWN.getHtml():VaadinIcons.CARET_RIGHT.getHtml()));
			
		propertySummary.append("<div class=\"annotation-panel-property-summary\">");
		if (!propertiesExpanded) {
			propertySummary.append(tag.getUserDefinedPropertyDefinitions().stream()
			.limit(3)
			.map(property -> Cleaner.clean(property.getName()))
			.collect(Collectors.joining(",")));
			propertySummary.append(
				((tag.getUserDefinedPropertyDefinitions().size() > 3)?"...":""));
		}
		propertySummary.append("</div>");
		return propertySummary.toString();
	}

	public boolean isPropertiesExpanded() {
		return propertiesExpanded;
	}

	public void setPropertiesExpanded(boolean propertiesExpanded) {
		this.propertiesExpanded = propertiesExpanded;
	}
	
	@Override
	public String getRemoveIcon() {
		if (editable) {
			return VaadinIcons.TRASH.getHtml();
		}
		return null;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}
	
	@Override
	public void handleRemovalRequest(TagsView tagsView) {
		if (editable) {
			tagsView.deleteTags(Collections.singletonList(tag));
		}
		
	} 
	
	@Override
	public String generateStyle() {
		return tag.isContribution()?"annotate-panel-tag-with-contributions":TagsetTreeItem.super.generateStyle();
	}
	
}
