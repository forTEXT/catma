package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.ui.Component;

import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.ui.tabbedview.TabbedView;

public class TagManagerView extends TabbedView {
	
	private PropertyChangeListener tagLibraryChangedListener;
	private TagManager tagManager;
	
	public TagManagerView(TagManager tagManager) {
		super ("There are no open Tag Libraries. " +
				"Please use the Repository Manager to open a Tag Libray.");
		
		this.tagManager = tagManager;
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == null) { // removal
					TagLibrary tagLibrary =
							(TagLibrary)evt.getOldValue();
					TagLibraryView tlv = getTagLibraryView(tagLibrary.getId());
					if (tlv != null) {
						onTabClose(tlv);
					}
				}
				else if (evt.getOldValue() != null) { //update 
					//TODO: update tab text
				}
				
			}
		};
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagLibraryChanged, tagLibraryChangedListener);
	}

	public void openTagLibrary(TagLibrary tagLibrary) {
		TagLibraryView tagLibraryView = getTagLibraryView(tagLibrary.getId());
		
		if (tagLibraryView != null) {
			setSelectedTab(tagLibraryView);
		}
		else {
			tagLibraryView = new TagLibraryView(tagManager, tagLibrary);
			addClosableTab(tagLibraryView, tagLibrary.getName());
			setSelectedTab(tagLibraryView);
		}
	}
	
	
	private TagLibraryView getTagLibraryView(String tagLibraryID) {
		for (Component tabContent : this) {
			TagLibraryView tagLibraryView = (TagLibraryView)tabContent;
			if (tagLibraryView.getTagLibrary().getId().equals(tagLibraryID)) {
				return tagLibraryView;
			}
		}
		return null;
	}
}
