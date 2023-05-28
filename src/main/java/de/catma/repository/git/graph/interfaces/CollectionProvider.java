package de.catma.repository.git.graph.interfaces;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.tag.TagLibrary;

import java.io.IOException;

public interface CollectionProvider {
    default AnnotationCollection getCollection(String collectionId, TagLibrary tagLibrary) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    default AnnotationCollection getCollection(String collectionId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
