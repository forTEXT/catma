package de.catma.ui.tagger;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;

public interface CurrentWritableUserMarkupCollectionProvider {
	
	public UserMarkupCollection getCurrentWritableUserMarkupCollection();

}
