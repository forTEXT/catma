package de.catma.ui.tagger.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CollectionDataItem implements DocumentTreeItem {

	private UserMarkupCollectionReference collectionRef;
	private boolean selected = true;

	public CollectionDataItem(UserMarkupCollectionReference collectionRef) {
		super();
		this.collectionRef = collectionRef;
	}
	
	@Override
	public String getSelectionIcon() {
		return selected?VaadinIcons.EYE.getHtml():VaadinIcons.EYE_SLASH.getHtml();
	}

	@Override
	public String getName() {
		return collectionRef.getName();
	}

	public UserMarkupCollectionReference getCollectionRef() {
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

}
