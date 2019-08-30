package de.catma.ui.tagger;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;

@Deprecated
public interface CurrentWritableUserMarkupCollectionProvider {
	
	public UserMarkupCollection getCurrentWritableUserMarkupCollection();

}
