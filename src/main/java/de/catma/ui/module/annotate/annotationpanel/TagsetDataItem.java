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
import de.catma.ui.util.Cleaner;

class TagsetDataItem implements TagsetTreeItem {
	
	private final TagsetDefinition tagset;
	private boolean visible;
	private boolean expanded = false;
	private final Collator collator;

	public TagsetDataItem(TagsetDefinition tagset, Collator collator) {
		super();
		this.tagset = tagset;
		this.collator = collator;
	}

	@Override
	public String getColor() {
		return getTagsetName();
	}

	@Override
	public String getName() {
		if (expanded) {
			return "";
		}
		StringBuilder tagSummeryBuilder = new StringBuilder();
		if (!tagset.isEmpty()) {
			List<TagDefinition> rootTags = tagset.getRootTagDefinitions();
			tagSummeryBuilder.append(
				rootTags.stream()
				.limit(3)
				.sorted((t1, t2)->collator.compare(Optional.ofNullable(t1.getName()).orElse(""), Optional.ofNullable(t2.getName()).orElse("")))
				.map(tag -> Cleaner.clean(tag.getName()))
				.collect(Collectors.joining(", ")));
			tagSummeryBuilder.append(
				((rootTags.size() > 3)?"...":""));
		}
		return tagSummeryBuilder.toString();
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
	public List<TagReference> getTagReferences(List<AnnotationCollection> collections) {
		List<TagReference> result = new ArrayList<>();
		
		for (AnnotationCollection collection : collections) {
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
		return null; // no properties for tagsets
	}
	
	@Override
	public String toString() {
		return tagset.getName();
	}

	@Override
	public void setTagsetExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
	@Override
	public String getId() {
		return tagset.getUuid();
	}
	
	@Override
	public int compareTo(TagsetTreeItem o) {
		if (!getTagset().getUuid().equals(o.getTagset().getUuid())) {
			return collator.compare(Optional.ofNullable(getTagset().getName()).orElse(""), Optional.ofNullable(o.getTagset().getName()).orElse(""));
		}
		return 1;
	}
	
	@Override
	public TagDefinition getTag() {
		return null; //intended
	}
	
	@Override
	public PropertyDefinition getPropertyDefinition() {
		return null; //intended
	}
}
