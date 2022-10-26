package de.catma.repository.git.graph.interfaces;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.tag.TagLibrary;

import java.io.IOException;

public interface CollectionProvider {
    AnnotationCollection get(String collectionId, TagLibrary tagLibrary) throws IOException;
}
