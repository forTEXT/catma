package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.vaadin.ui.Component;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagManager;
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
			TagManager tagManager, final SourceDocument sourceDocument, Repository repository) {
		TaggerView taggerView = getTaggerView(sourceDocument);
		if (taggerView != null) {
			setSelectedTab(taggerView);
		}
		else {
			taggerView = new TaggerView(
					nextTaggerID++, tagManager, sourceDocument, repository,
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
								//TODO: update Tab Title
							}
							
						}
					});
			addClosableTab(taggerView, sourceDocument.toString());
			setSelectedTab(taggerView);
		}
		
		return taggerView;
	}
	
	
	private TaggerView getTaggerView(SourceDocument sourceDocument) {
		for (Component tabContent : this) {
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
