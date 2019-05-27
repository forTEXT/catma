package de.catma.ui.analyzenew.resourcepanelanalyze;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.ui.analyzenew.resourcepanelanalyze.AnalyzeResourceSelectionListener;

public class CollectionDataItem implements DocumentTreeItem {

	private UserMarkupCollectionReference collectionRef;
	private boolean selected = true;

	public CollectionDataItem(UserMarkupCollectionReference collectionRef, boolean selected) {
		super();
		this.collectionRef = collectionRef;
		this.selected = selected;
	}
	
	@Override
	public String getSelectionIcon() {
		return selected?VaadinIcons.DOT_CIRCLE.getHtml():VaadinIcons.CIRCLE_THIN.getHtml();
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
	public void fireSelectedEvent(AnalyzeResourceSelectionListener analyzeResourceSelectionListener, boolean selected) {
		analyzeResourceSelectionListener.resourceSelected(collectionRef,selected);
		
	}


}
