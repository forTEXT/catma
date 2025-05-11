package de.catma.ui.module.annotate.annotationpanel;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.vaadin.data.provider.TreeDataProvider;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

class PossibleValueDataItem implements TagsetTreeItem {
	
	private final String value;
	private final PropertyDefinition propertyDefinition;
	private final TagDefinition tag;
	private final TagsetDefinition tagset;
	private final Collator collator;
	
	public PossibleValueDataItem(String value, PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, Collator collator) {
		super();
		this.value = value;
		this.propertyDefinition = propertyDefinition;
		this.tag = tag;
		this.tagset = tagset;
		this.collator = collator;
	}

	@Override
	public String getColor() {
		return null; //intended
	}

	@Override
	public String getName() {
		return null; //intended
	}

	@Override
	public String getTagsetName() {
		return null; //intended
	}

	@Override
	public String getVisibilityIcon() {
		return null; //intended
	}

	@Override
	public String getPropertySummary() {
		return null;
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public void setVisible(boolean visible) {
		// nooop
	}

	@Override
	public List<TagReference> getTagReferences(List<AnnotationCollection> collections) {
		return Collections.emptyList();
	}

	@Override
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> treeDataProvider, boolean visible,
			boolean explicit) {
		// noop
	}

	@Override
	public void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {
		dataProvider.getTreeData().removeItem(this);
	}
	
	@Override
	public String generateStyle() {
		return "annotate-panel-possible-value-data-item";
	}
	
	@Override
	public String getPropertyValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	@Override
	public String getId() {
		return value;
	}
	
	@Override
	public TagsetDefinition getTagset() {
		return tagset;
	}
	
	@Override
	public TagDefinition getTag() {
		return tag;
	}
	
	@Override
	public PropertyDefinition getPropertyDefinition() {
		return propertyDefinition;
	}

	@Override
	public int compareTo(TagsetTreeItem o) {
		if (!getTagset().getUuid().equals(o.getTagset().getUuid())) {
			return collator.compare(Optional.ofNullable(getTagset().getName()).orElse(""), Optional.ofNullable(o.getTagset().getName()).orElse(""));
		}

		if (getTag() != null && !getTag().getUuid().equals(o.getTag().getUuid())) {
			return collator.compare(Optional.ofNullable(getTag().getName()).orElse(""), Optional.ofNullable(o.getTag().getName()).orElse(""));
		}
		
		if (getPropertyDefinition() != null && !getPropertyDefinition().getUuid().equals(o.getPropertyDefinition().getUuid())) {
			return collator.compare(Optional.ofNullable(getPropertyDefinition().getName()).orElse(""), Optional.ofNullable(o.getPropertyDefinition().getName()).orElse(""));
		}
		
		if ((getTag() == null) || getPropertyDefinition() == null) {
			return -1;
		}
		
		return collator.compare(Optional.ofNullable(value).orElse(""), Optional.ofNullable(o.getPropertyValue()).orElse(""));
	}
	
}
