package de.catma.ui.module.tags;

import java.text.Collator;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.icons.VaadinIcons;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.util.HtmlEscaper;
import de.catma.util.ColorConverter;

class TagDataItem implements TagsetTreeItem {
	
	private final TagDefinition tag;
	private final boolean editable;
	private final TagsetDefinition tagset;
	private final Collator collator;
	private boolean propertiesExpanded;
	
	public TagDataItem(TagDefinition tag, TagsetDefinition tagset, Collator collator) {
		this(tag, tagset, false, collator);
	}

	public TagDataItem(TagDefinition tag, TagsetDefinition tagset, boolean editable, Collator collator) {
		super();
		this.tag = tag;
		this.tagset = tagset;
		this.editable = editable;
		this.collator = collator;
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
			.sorted((p1,p2)->collator.compare(Optional.ofNullable(p1.getName()).orElse(""), Optional.ofNullable(p2.getName()).orElse("")))
			.map(property -> HtmlEscaper.escape(property.getName()))
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

	@Override
	public TagsetDefinition getTagset() {
		return tagset;
	}
	
	@Override
	public PropertyDefinition getPropertyDefinition() {
		return null; // intended
	}
	
	@Override
	public int compareTo(TagsetTreeItem o) {
		
		if (!getTagset().getUuid().equals(o.getTagset().getUuid())) {
			return collator.compare(Optional.ofNullable(getTagset().getName()).orElse(""), Optional.ofNullable(o.getTagset().getName()).orElse(""));
		}
		
		if (getTag() == null) {
			return -1;
		}

		return collator.compare(Optional.ofNullable(getTag().getName()).orElse(""), Optional.ofNullable(o.getTag().getName()).orElse(""));		
	}
}
