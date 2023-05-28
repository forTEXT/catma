package de.catma.ui.module.annotate.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.AnnotationCollectionReference;

class CollectionDataItem implements DocumentTreeItem {

	private AnnotationCollectionReference collectionRef;
	private boolean selected = true;

	public CollectionDataItem(
			AnnotationCollectionReference collectionRef) {
		this(collectionRef, true);
	}
	
	public CollectionDataItem(
			AnnotationCollectionReference collectionRef, 
			boolean selected) {
		this.collectionRef = collectionRef;
		this.selected = selected;
	}
	
	@Override
	public String getSelectionIcon() {
		return selected?VaadinIcons.EYE.getHtml():VaadinIcons.EYE_SLASH.getHtml();
	}

	@Override
	public String getName() {
		return collectionRef.getName();
	}

	public AnnotationCollectionReference getCollectionRef() {
		return collectionRef;
	}
	
	@Override
	public String getIcon() {
		return VaadinIcons.NOTEBOOK.getHtml();
	}

	@Override
	public void setSelected(boolean value) {
		this.selected = value;
	}
	
	@Override
	public boolean isSingleSelection() {
		return false;
	}
	
	@Override
	public boolean isSelected() {
		return selected;
	}
	
	@Override
	public void fireSelectedEvent(ResourceSelectionListener resourceSelectionListener) {
		resourceSelectionListener.annotationCollectionSelected(collectionRef, isSelected());
	}
	
	@Override
	public String toString() {
		return collectionRef.getName();
	}
}
