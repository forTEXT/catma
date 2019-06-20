package de.catma.ui.tagger.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

class CollectionDataItem implements DocumentTreeItem {

	private UserMarkupCollectionReference collectionRef;
	private boolean selected = true;
	private boolean hasWritePermission;

	public CollectionDataItem(UserMarkupCollectionReference collectionRef, boolean hasWritePermission) {
		super();
		this.collectionRef = collectionRef;
		this.hasWritePermission = hasWritePermission;
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
	
	@Override
	public String toString() {
		return collectionRef.getName();
	}

	@Override
	public String getPermissionIcon() {
		return hasWritePermission?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml();
	}
}
