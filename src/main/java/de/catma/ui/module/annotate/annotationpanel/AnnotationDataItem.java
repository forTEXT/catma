package de.catma.ui.module.annotate.annotationpanel;

import java.util.function.Supplier;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.Annotation;
import de.catma.indexer.KwicProvider;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class AnnotationDataItem implements AnnotationTreeItem {
	private Annotation annotation;
	private String annotatedText;
	private String keywordInContext;
	private TagsetDefinition tagset;
	private Supplier<Boolean> inCurrentCollectionSupplier;
	private Supplier<String> descriptionSupplier;
	
	public AnnotationDataItem(
		Annotation annotation, TagsetDefinition tagset, KwicProvider kwicProvider, 
		boolean editable, Supplier<Boolean> inCurrentCollectionSupplier, Supplier<Integer> descriptionContextSizeSupplier) {
		super();
		this.annotation = annotation;
		this.tagset = tagset;
		this.inCurrentCollectionSupplier = inCurrentCollectionSupplier;
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
	public String getTag() {
		TagDefinition tag = tagset.getTagDefinition(annotation.getTagInstance().getTagDefinitionId());
		if (tag != null) {
			return tag.getName();
		}
		else {
			return "N/A";
		}
	}

	@Override
	public String getAuthor() {
		return annotation.getTagInstance().getAuthor();
	}

	@Override
	public String getCollection() {
		return annotation.getUserMarkupCollection().getName();
	}

	@Override
	public String getTagset() {
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
	
	public Annotation getAnnotation() {
		return annotation;
	}
	
}
