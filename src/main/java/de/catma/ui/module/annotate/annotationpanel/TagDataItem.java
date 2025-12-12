package de.catma.ui.module.annotate.annotationpanel;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.util.HtmlEscaper;
import de.catma.util.ColorConverter;

class TagDataItem implements TagsetTreeItem {
	
	private final TagDefinition tag;
	private boolean visible;
	private boolean propertiesExpanded;
	private final TagsetDefinition tagset;
	private final Collator collator;
	
	public TagDataItem(TagDefinition tag, TagsetDefinition tagset, Collator collator) {
		super();
		this.tag = tag;
		this.tagset = tagset;
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
	public String getVisibilityIcon() {
		return visible?VaadinIcons.EYE.getHtml():VaadinIcons.EYE_SLASH.getHtml();
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
	public boolean isVisible() {
		return visible;
	}
	
	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	@Override
	public List<TagReference> getTagReferences(List<AnnotationCollection> collections) {
		List<TagReference> result = new ArrayList<>();
		
		for (AnnotationCollection collection : collections) {
			result.addAll(collection.getTagReferences(tag));
		}
		
		return result;
	}
	
	@Override
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> dataProvider, boolean visible, boolean explicit) {
		if (explicit) {
			for (TagsetTreeItem tagTreeItem : dataProvider.getTreeData().getChildren(this)) {
				setChildrenVisible(tagTreeItem, visible, dataProvider);
			}
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
	public String getId() {
		return tag.getUuid();
	}
	
	@Override
	public TagsetDefinition getTagset() {
		return tagset;
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
	
	@Override
	public PropertyDefinition getPropertyDefinition() {
		return null; //intended
	}
}
