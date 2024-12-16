package de.catma.ui.module.tags;

import java.text.Collator;
import java.util.Optional;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

class PossibleValueDataItem implements TagsetTreeItem {
	
	private final String value;
	private final PropertyDefinition propertyDefinition;
	private final TagDefinition tag;
	private final TagsetDefinition tagset;
	private final boolean editable;
	private final Collator collator;

	
	public PossibleValueDataItem(String value, PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, boolean editable, Collator collator) {
		super();
		this.value = value;
		this.propertyDefinition = propertyDefinition;
		this.tag = tag;
		this.tagset = tagset;
		this.editable = editable;
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
	public String getPropertySummary() {
		return null;
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
			tagsView.deletePossibleValueDataItem(this);
		}
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
