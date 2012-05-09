package de.catma.ui.tagger;

import com.vaadin.ui.Component;

import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagManager;
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
			TagManager tagManager, SourceDocument sourceDocument, Repository repository) {
		TaggerView taggerView = getTaggerView(sourceDocument);
		if (taggerView != null) {
			setSelectedTab(taggerView);
		}
		else {
			taggerView = new TaggerView(
					nextTaggerID++, tagManager, sourceDocument, repository);
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
