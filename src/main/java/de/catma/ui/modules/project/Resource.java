package de.catma.ui.modules.project;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

/**
 * A wrapper interface for {@link SourceDocument}s or {@link UserMarkupCollectionReference}s
 * Only used in {@link ProjectView}
 *
 * @author db
 */
public interface Resource {

    String toString();

    String detail();

    boolean hasDetail();
}
