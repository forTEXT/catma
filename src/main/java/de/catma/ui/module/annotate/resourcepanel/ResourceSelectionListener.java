package de.catma.ui.module.annotate.resourcepanel;

import java.util.Collection;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.tag.TagsetDefinition;

public interface ResourceSelectionListener {
	public void documentSelected(SourceDocumentReference sourceDocumentReference);
	public void annotationCollectionSelected(
			AnnotationCollectionReference collectionReference, boolean selected);
	public void tagsetsSelected(Collection<TagsetDefinition> tagsets);
	public void resourcesChanged();
}
