package de.catma.ui.modules.project;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

/**
 * A wrapper interface for {@link SourceDocument}s or {@link UserMarkupCollectionReference}s
 * Only used in {@link ProjectView}
 *
 * @author db
 */
public interface Resource {

    String getName();

    String getDetail();

    boolean hasDetail();
    
	public String getIcon();

	public void deleteFrom(Repository project) throws Exception;
	
	public default boolean isCollection() { return false; };

}
