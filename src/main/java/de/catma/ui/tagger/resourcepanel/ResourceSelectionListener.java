package de.catma.ui.tagger.resourcepanel;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.tag.TagsetDefinition;

public interface ResourceSelectionListener {
	public void documentSelected(SourceDocument sourceDocument);
	public void annotationCollectionSelected(
			UserMarkupCollectionReference collectionReference, boolean selected);
	public void tagsetSelected(TagsetDefinition tagset, boolean selected);
}
