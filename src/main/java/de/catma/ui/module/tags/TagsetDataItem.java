package de.catma.ui.module.tags;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.icons.VaadinIcons;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.util.Cleaner;

class TagsetDataItem implements TagsetTreeItem {
	
	private final TagsetDefinition tagset;
	private boolean editable;
	private boolean expanded = false;
	private String responsibleUser;
	private final Collator collator;

	public TagsetDataItem(TagsetDefinition tagset, Collator collator) {
		this(tagset, null, false, collator);
	}
	
	public TagsetDataItem(TagsetDefinition tagset, String responsibleUser, boolean editable, Collator collator) {
		super();
		this.tagset = tagset;
		this.editable = editable;
		this.responsibleUser = responsibleUser;
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
		return "<span class=\"annotate-panel-tagsetname\">"+Cleaner.clean(tagset.getName())+"</span>";
	}

	@Override
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
		return null; // no properties for tagsets
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
		return null;
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
	
	@Override
	public void setTagsetExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
	@Override
	public String getResponsibleUser() {
		return responsibleUser;
	}
	
	@Override
	public String generateStyle() {
		return tagset.isContribution()? "annotate-panel-tagset-with-contributions" : TagsetTreeItem.super.generateStyle();
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
