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
package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.ui.Component;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.ui.tabbedview.TabbedView;

public class TaggerManagerView extends TabbedView {
	
	private int nextTaggerID = 0;
	
	public TaggerManagerView() {
		super(
			"There are no open Taggers. " +
			"Please use the Repository Manager " +
			"to open a Source Document in a Tagger.");
	}

	public TaggerView openSourceDocument(
			final SourceDocument sourceDocument, Repository repository) {
		TaggerView taggerView = getTaggerView(sourceDocument);
		if (taggerView != null) {
			setSelectedTab(taggerView);
		}
		else {
			taggerView = new TaggerView(
					nextTaggerID++, sourceDocument, repository,
					new PropertyChangeListener() {
						
						public void propertyChange(PropertyChangeEvent evt) {

							if (evt.getNewValue() == null) { //remove
								SourceDocument sd = (SourceDocument) evt.getOldValue();
								if (sd.getID().equals(sourceDocument.getID())) {
									TaggerView taggerView = 
											getTaggerView(sourceDocument);
									if (taggerView != null) {
										onTabClose(taggerView);
									}
								}
							}
							else if (evt.getOldValue() != null) { //update
								String sdID = (String) evt.getOldValue();
								if (sdID.equals(sourceDocument.getID())) {
									TaggerView taggerView = 
											getTaggerView(sourceDocument);
									if (taggerView != null) {
										taggerView.setSourceDocument(
												(SourceDocument) evt.getNewValue());
									}
								}								
							}
							
						}
					});
			addClosableTab(taggerView, sourceDocument.toString());
			setSelectedTab(taggerView);
		}
		
		return taggerView;
	}
	
	
	private TaggerView getTaggerView(SourceDocument sourceDocument) {
		for (Component tabContent : this.getTabSheet()) {
			TaggerView taggerView = (TaggerView)tabContent;
			if (taggerView.getSourceDocument().getID().equals(
					sourceDocument.getID())) {
				return taggerView;
			}
		}
		
		return null;
	}

	public void openUserMarkupCollection(TaggerView taggerView,
			UserMarkupCollection userMarkupCollection) {
		taggerView.openUserMarkupCollection(userMarkupCollection);
	}
}
