package de.catma.ui.module.annotate.annotationpanel;

import java.text.Collator;
import java.util.Objects;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class AnnotationPropertyValueDataItem implements AnnotationTreeItem {
	
	private final String value;
	private final String annotationId;
	private final String sortableAuthor;
	private final PropertyDefinition propertyDefinition;
	private final TagDefinition tag;
	private final TagsetDefinition tagset;
	private final AnnotationCollection collection;
	private final Collator collator;

	public AnnotationPropertyValueDataItem(String value, String annotationId, String sortableAuthor, 
			PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, AnnotationCollection collection, 
			Collator collator) {
		super();
		this.value = value;
		this.annotationId = annotationId;
		this.sortableAuthor = sortableAuthor;
		this.propertyDefinition = propertyDefinition;
		this.tag = tag;
		this.tagset = tagset;
		this.collection = collection;
		this.collator = collator;
	}

	@Override
	public String getDetail() {
		if (value.length() > MAX_VALUE_LENGTH) {
			return shorten(value, MAX_VALUE_LENGTH);
		}
		return value;
	}

	@Override
	public String getDescription() {
		return value;
	}
	
	@Override
	public String getAnnotationId() {
		return annotationId;
	}
	
	@Override
	public PropertyDefinition getPropertyDefinition() {
		return propertyDefinition;
	}

	@Override
	public int compareTo(AnnotationTreeItem o) {
		if (Objects.equals(this.getAnnotationId(), o.getAnnotationId())) {
			if (Objects.equals(this.getPropertyDefinition().getUuid(), o.getPropertyDefinition().getUuid())) {
				return collator.compare(getDescription(), o.getDescription());
			}
			else {
				return collator.compare(getPropertyDefinition()==null?"":getPropertyDefinition().getName(), o.getPropertyDefinition()==null?"":o.getPropertyDefinition().getName());
			}
		}
		else {
			return collator.compare(getDescription(), o.getDescription());
		}
	}

	@Override
	public TagDefinition getTag() {
		return tag;
	}
	
	@Override
	public String getSortableAuthor() {
		return sortableAuthor;
	}
	
	@Override
	public AnnotationCollection getCollection() {
		return collection;
	}
	
	@Override
	public TagsetDefinition getTagset() {
		return tagset;
	}
	
	@Override
	public String getSortableTagPath() {
		if (tag != null) {
			return tag.getName();
		}
		else {
			return "N/A";
		}	
	}
	
	@Override
	public int compareToByTag(AnnotationTreeItem o) {
		if (Objects.equals(this.getAnnotationId(), o.getAnnotationId())) {
			if (Objects.equals(this.getPropertyDefinition().getUuid(), o.getPropertyDefinition().getUuid())) {
				return collator.compare(getDescription(), o.getDescription());
			}
			else {
				return collator.compare(getPropertyDefinition()==null?"":getPropertyDefinition().getName(), o.getPropertyDefinition()==null?"":o.getPropertyDefinition().getName());
			}
		}
		else {
			TagDefinition oTag = o.getTag();
			if (tag != null && oTag != null && Objects.equals(tag, oTag)) {
				return tag.getUuid().compareTo(oTag.getUuid());
			}
			return collator.compare(getTag() != null?getTag().getName():"", o.getTag() != null?o.getTag().getName():"");
		}
	}
	
	@Override
	public int compareToByTagPath(AnnotationTreeItem o) {
		if (Objects.equals(this.getAnnotationId(), o.getAnnotationId())) {
			if (Objects.equals(this.getPropertyDefinition().getUuid(), o.getPropertyDefinition().getUuid())) {
				return collator.compare(getDescription(), o.getDescription());
			}
			else {
				return collator.compare(getPropertyDefinition()==null?"":getPropertyDefinition().getName(), o.getPropertyDefinition()==null?"":o.getPropertyDefinition().getName());
			}
		}
		
		return collator.compare(getSortableTagPath() != null?getSortableTagPath():"", o.getSortableTagPath() != null?o.getSortableTagPath():"");

	}
	
	@Override
	public int compareToByAuthor(AnnotationTreeItem o) {
		if (Objects.equals(this.getAnnotationId(), o.getAnnotationId())) {
			if (Objects.equals(this.getPropertyDefinition().getUuid(), o.getPropertyDefinition().getUuid())) {
				return collator.compare(getDescription(), o.getDescription());
			}
			else {
				return collator.compare(getPropertyDefinition()==null?"":getPropertyDefinition().getName(), o.getPropertyDefinition()==null?"":o.getPropertyDefinition().getName());
			}
		}
		
		return collator.compare(getSortableAuthor() != null?getSortableAuthor():"", o.getSortableAuthor() != null?o.getSortableAuthor():"");

	}

	@Override
	public int compareToByCollection(AnnotationTreeItem o) {
		if (Objects.equals(this.getAnnotationId(), o.getAnnotationId())) {
			if (Objects.equals(this.getPropertyDefinition().getUuid(), o.getPropertyDefinition().getUuid())) {
				return collator.compare(getDescription(), o.getDescription());
			}
			else {
				return collator.compare(getPropertyDefinition()==null?"":getPropertyDefinition().getName(), o.getPropertyDefinition()==null?"":o.getPropertyDefinition().getName());
			}
		}
		else {
			AnnotationCollection oCollection = o.getCollection();
			if (collection != null && oCollection != null && Objects.equals(collection, oCollection)) {
				return collection.getUuid().compareTo(oCollection.getUuid());
			}
			return collator.compare(getCollection() != null?getCollection().getName():"", o.getCollection() != null?o.getCollection().getName():"");
		}
	}
	
	@Override
	public int compareToByTagset(AnnotationTreeItem o) {
		if (Objects.equals(this.getAnnotationId(), o.getAnnotationId())) {
			if (Objects.equals(this.getPropertyDefinition().getUuid(), o.getPropertyDefinition().getUuid())) {
				return collator.compare(getDescription(), o.getDescription());
			}
			else {
				return collator.compare(getPropertyDefinition()==null?"":getPropertyDefinition().getName(), o.getPropertyDefinition()==null?"":o.getPropertyDefinition().getName());
			}
		}
		else {
			TagsetDefinition oTagset = o.getTagset();
			if (tagset != null && oTagset != null && Objects.equals(tagset, oTagset)) {
				return tagset.getUuid().compareTo(oTagset.getUuid());
			}
			return collator.compare(getTagset() != null?getTagset().getName():"", o.getTagset() != null?o.getTagset().getName():"");
		}
	}
}
