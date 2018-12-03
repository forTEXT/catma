package de.catma.ui.tagger.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CollectionDataItem implements DocumentTreeItem {

	private UserMarkupCollectionReference collectionRef;
	private boolean selected;

	public CollectionDataItem(UserMarkupCollectionReference collectionRef) {
		super();
		this.collectionRef = collectionRef;
	}
	
	@Override
	public String getSelectionIcon() {
		return selected?VaadinIcons.CHECK_SQUARE_O.getHtml():VaadinIcons.THIN_SQUARE.getHtml();
	}

	@Override
	public String getName() {
		return collectionRef.getName();
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

}
