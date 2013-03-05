/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
