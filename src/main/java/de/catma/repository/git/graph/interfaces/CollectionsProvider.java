package de.catma.repository.git.graph.interfaces;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.tag.TagLibrary;

import java.io.IOException;
import java.util.List;

public interface CollectionsProvider {
    List<AnnotationCollection> get(TagLibrary tagLibrary) throws IOException;
}
