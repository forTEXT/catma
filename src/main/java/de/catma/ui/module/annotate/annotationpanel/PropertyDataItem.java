package de.catma.ui.module.annotate.annotationpanel;

import java.text.Collator;
import java.util.Collections;
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
import de.catma.ui.util.Cleaner;

class PropertyDataItem implements TagsetTreeItem {
	
	private final PropertyDefinition propertyDefinition;
	private boolean valuesExpanded;
	private final TagDefinition tag;
	private final TagsetDefinition tagset;
	private final Collator collator;
	
	public PropertyDataItem(PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, Collator collator) {
		super();
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
		StringBuilder propertySummary = new StringBuilder();
		if (!propertyDefinition.getPossibleValueList().isEmpty()) {
			propertySummary.append(
				(valuesExpanded?VaadinIcons.CARET_DOWN.getHtml():VaadinIcons.CARET_RIGHT.getHtml()));
		}
			
		propertySummary.append("<div class=\"annotation-panel-property-summary\">");
		propertySummary.append(Cleaner.clean(propertyDefinition.getName())); 

		if (!valuesExpanded) {
			if (!propertyDefinition.getPossibleValueList().isEmpty()) { 
				propertySummary.append(" - ");
			}
			propertySummary.append(propertyDefinition.getPossibleValueList().stream()
			.limit(2)
			.sorted((v1, v2) -> collator.compare(Optional.ofNullable(v1).orElse(""), Optional.ofNullable(v2).orElse("")))
			.map(pValue -> Cleaner.clean(pValue))
			.collect(Collectors.joining(",")));
			propertySummary.append(
				((propertyDefinition.getPossibleValueList().size() > 2)?"...":""));
		}
		propertySummary.append("</div>");
		return propertySummary.toString();		
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
		return "annotate-panel-property-data-item";
	}
	
	public PropertyDefinition getPropertyDefinition() {
		return propertyDefinition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((propertyDefinition == null) ? 0 : propertyDefinition.hashCode());
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
		PropertyDataItem other = (PropertyDataItem) obj;
		if (propertyDefinition == null) {
			if (other.propertyDefinition != null)
				return false;
		} else if (!propertyDefinition.equals(other.propertyDefinition))
			return false;
		return true;
	}
	
	public boolean isValuesExpanded() {
		return valuesExpanded;
	}
	
	public void setValuesExpanded(boolean valuesExpanded) {
		this.valuesExpanded = valuesExpanded;
	}
	
	@Override
	public String toString() {
		return propertyDefinition.getName();
	}
	
	@Override
	public String getId() {
		return propertyDefinition.getUuid();
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
	public int compareTo(TagsetTreeItem o) {
		
		if (!getTagset().getUuid().equals(o.getTagset().getUuid())) {
			return collator.compare(Optional.ofNullable(getTagset().getName()).orElse(""), Optional.ofNullable(o.getTagset().getName()).orElse(""));
		}

		if (o.getTag() != null && !getTag().getUuid().equals(o.getTag().getUuid())) {
			return collator.compare(Optional.ofNullable(getTag().getName()).orElse(""), Optional.ofNullable(o.getTag().getName()).orElse(""));
		}
		
		if ((getTag() == null) || getPropertyDefinition() == null) {
			return -1;
		}
		
		return collator.compare(Optional.ofNullable(getPropertyDefinition().getName()).orElse(""), Optional.ofNullable(o.getPropertyDefinition().getName()).orElse(""));
	}
}
