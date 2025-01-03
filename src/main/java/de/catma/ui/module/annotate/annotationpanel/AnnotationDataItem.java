package de.catma.ui.module.annotate.annotationpanel;

import java.text.Collator;
import java.util.Objects;
import java.util.function.Supplier;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.Annotation;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.indexer.KwicProvider;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class AnnotationDataItem implements AnnotationTreeItem {
	private final Annotation annotation;
	private final String annotatedText;
	private String keywordInContext;
	private final TagsetDefinition tagset;
	private final Supplier<Boolean> inCurrentCollectionSupplier;
	private final Supplier<String> descriptionSupplier;
	private final Collator collator;
	
	public AnnotationDataItem(
		Annotation annotation, TagsetDefinition tagset, KwicProvider kwicProvider, 
		boolean editable, Supplier<Boolean> inCurrentCollectionSupplier, Supplier<Integer> descriptionContextSizeSupplier, Collator collator) {
		super();
		this.annotation = annotation;
		this.tagset = tagset;
		this.inCurrentCollectionSupplier = inCurrentCollectionSupplier;
		this.collator = collator;
		TagDefinition tagDefinition = 
			tagset.getTagDefinition(annotation.getTagInstance().getTagDefinitionId());
		this.annotatedText = AnnotatedTextProvider.buildAnnotatedText(
				annotation.getTagReferences(), 
				kwicProvider, 
				tagDefinition,
				0);
		this.descriptionSupplier = () -> AnnotatedTextProvider.buildAnnotatedKeywordInContext(
				annotation.getTagReferences(), kwicProvider, tagDefinition, tagset.getTagPath(tagDefinition),
				descriptionContextSizeSupplier.get());
		refreshDescription();
	}
	
	@Override
	public void refreshDescription() {
		this.keywordInContext = this.descriptionSupplier.get();
	}
	
	@Override
	public String getDetail() {
		return annotatedText;
	}

	@Override
	public String getTagName() {
		TagDefinition tag = tagset.getTagDefinition(annotation.getTagInstance().getTagDefinitionId());
		if (tag != null) {
			return tag.getName();
		}
		else {
			return "N/A";
		}
	}
	
	@Override
	public String getTagPath() {
		TagDefinition tag = tagset.getTagDefinition(annotation.getTagInstance().getTagDefinitionId());
		if (tag != null) {
			return tagset.getTagPath(tag);
		}
		return "N/A";
	}

	@Override
	public String getAuthor() {
		return annotation.getTagInstance().getAuthor();
	}

	@Override
	public String getCollectionName() {
		return annotation.getUserMarkupCollection().getName();
	}

	@Override
	public String getTagsetName() {
		return tagset.getName();
	}

	@Override
	public String getAnnotationId() {
		return annotation.getTagInstance().getUuid();
	}
	
	@Override
	public String getDescription() {
		return keywordInContext;
	}

	@Override
	public String getEditIcon() {
		if (inCurrentCollectionSupplier.get()) {
			return VaadinIcons.EDIT.getHtml();
		}
		else {
			return null;
		}
	}
	
	@Override
	public String getDeleteIcon() {
		if (inCurrentCollectionSupplier.get()) {
			return VaadinIcons.TRASH.getHtml();
		}
		else {
			return VaadinIcons.EXCHANGE.getHtml() + VaadinIcons.NOTEBOOK.getHtml();
		}
	}
	
	@Override
	public TagDefinition getTag() {
		return tagset.getTagDefinition(annotation.getTagInstance().getTagDefinitionId());
	}
	
	@Override
	public TagsetDefinition getTagset() {
		return tagset;
	}
	
	public Annotation getAnnotation() {
		return annotation;
	}
	
	@Override
	public String getSortableAuthor() {
		return getAuthor();
	}

	@Override
	public AnnotationCollection getCollection() {
		return annotation.getUserMarkupCollection();
	}
	
	@Override
	public String getSortableTagPath() {
		return getTagPath();
	}
	
	@Override
	public int compareTo(AnnotationTreeItem o) {
		return collator.compare(getDescription(), o.getDescription());
	}
	
	@Override
	public int compareToByTag(AnnotationTreeItem o) {
		TagDefinition oTag = o.getTag();
		TagDefinition tag = getTag();
		if (tag != null && oTag != null && Objects.equals(tag, oTag)) {
			return tag.getUuid().compareTo(oTag.getUuid());
		}
		return collator.compare(getTag() != null?getTag().getName():"", o.getTag() != null?o.getTag().getName():"");
	}
	
	@Override
	public int compareToByTagPath(AnnotationTreeItem o) {
		return collator.compare(getSortableTagPath() != null?getSortableTagPath():"", o.getSortableTagPath() != null?o.getSortableTagPath():"");
	}

	@Override
	public int compareToByAuthor(AnnotationTreeItem o) {
		return collator.compare(getSortableAuthor() != null?getSortableAuthor():"", o.getSortableAuthor() != null?o.getSortableAuthor():"");
	}
	
	@Override
	public int compareToByCollection(AnnotationTreeItem o) {
		AnnotationCollection oCollection = o.getCollection();
		AnnotationCollection collection = getCollection();
		if (collection != null && oCollection != null && Objects.equals(collection, oCollection)) {
			return collection.getUuid().compareTo(oCollection.getUuid());
		}
		return collator.compare(getCollectionName() != null?getCollection().getName():"", o.getCollection() != null?o.getCollection().getName():"");
	}
	
	@Override
	public int compareToByTagset(AnnotationTreeItem o) {
		TagsetDefinition oTagset = o.getTagset();
		TagsetDefinition tagset = getTagset();
		if (tagset != null && oTagset != null && Objects.equals(tagset, oTagset)) {
			return tagset.getUuid().compareTo(oTagset.getUuid());
		}
		return collator.compare(getTagset() != null?getTagset().getName():"", o.getTagset() != null?o.getTagset().getName():"");
	}
	
}
