package de.catma.ui.tagger;

import com.vaadin.ui.Component;

import de.catma.document.repository.Repository;
import de.catma.document.source.ISourceDocument;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.IUserMarkupCollection;
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
			TagManager tagManager, ISourceDocument sourceDocument, Repository repository) {
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
	
	
	private TaggerView getTaggerView(ISourceDocument sourceDocument) {
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
			IUserMarkupCollection userMarkupCollection) {
		taggerView.openUserMarkupCollection(userMarkupCollection);
	}
}
