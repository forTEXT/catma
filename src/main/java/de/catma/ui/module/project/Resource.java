package de.catma.ui.module.project;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.project.Project;

/**
 * A wrapper interface for {@link SourceDocument}s or {@link AnnotationCollectionReference}s
 * Only used in {@link ProjectView}
 *
 * @author db
 */
public interface Resource {

	
	/**
	 * 
	 * @return the resource identifier e.g. catma uuid
	 */
	String getResourceId();

	
	/**
	 * 
	 * @return the project identifier
	 */
	String getProjectId();
	
	
    String getName();

    String getDetail();

    boolean hasDetail();
    
	public String getIcon();

	public void deleteFrom(Project project) throws Exception;
	
	public default boolean isCollection() { return false; }

	public String getPermissionIcon();
	
	public boolean hasWritePermission();

}
